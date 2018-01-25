package io.sweers.moshkt.compiler

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.sweers.moshkt.api.MoshiSerializable
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.KotlinMetadataUtils
import me.eugeniomarletti.kotlin.metadata.declaresDefaultValue
import me.eugeniomarletti.kotlin.metadata.extractFullName
import me.eugeniomarletti.kotlin.metadata.isDataClass
import me.eugeniomarletti.kotlin.metadata.isPrimary
import me.eugeniomarletti.kotlin.metadata.kaptGeneratedOption
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import me.eugeniomarletti.kotlin.metadata.visibility
import me.eugeniomarletti.kotlin.processing.KotlinAbstractProcessor
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.Visibility
import org.jetbrains.kotlin.serialization.ProtoBuf.Visibility.INTERNAL
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic.Kind.ERROR

/**
 * An annotation processor that reads Kotlin data classes and generates Moshi JsonAdapters for them.
 * This generates Kotlin code, and understands basic Kotlin language features like default values
 * and companion objects.
 *
 * The generated class will match the visibility of the given data class (i.e. if it's internal, the
 * adapter will also be internal).
 *
 * If you define a companion object, a jsonAdapter() extension function will be generated onto it.
 * If you don't want this though, you can use the runtime [MoshiSerializable] factory implementation.
 *
 * Things that are implemented:
 *   * Standard, parameterized, and wildcard types
 *
 * Things that are not implemented yet:
 *   * Generics support
 *   * Leveraging default values on params where possible. This might not be feasible though.
 */
@AutoService(Processor::class)
class MoshiKtProcessor : KotlinAbstractProcessor(), KotlinMetadataUtils {

  private val annotationName = MoshiSerializable::class.java.canonicalName

  override fun getSupportedAnnotationTypes() = setOf(annotationName)

  override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val annotationElement = elementUtils.getTypeElement(annotationName)
    return if (roundEnv.getElementsAnnotatedWith(annotationElement)
            .asSequence()
            .mapNotNull { processElement(it) }
            .any { !it.generateAndWrite() }) true else true
  }

  private fun processElement(element: Element): ClassToAdapt? {
    val metadata = element.kotlinMetadata

    if (metadata !is KotlinClassMetadata) {
      errorMustBeDataClass(element)
      return null
    }

    val classData = metadata.data
    val (nameResolver, classProto) = classData

    fun ProtoBuf.Type.extractFullName() = extractFullName(classData)

    if (!classProto.isDataClass) {
      errorMustBeDataClass(element)
      return null
    }

    val fqClassName = nameResolver.getString(classProto.fqName).replace('/', '.')

    val packageName = nameResolver.getString(classProto.fqName).substringBeforeLast('/').replace(
        '/', '.')

    val hasCompanionObject = classProto.hasCompanionObjectName()
    println("Has companion object? $hasCompanionObject")

    val parameters = classProto.constructorList
        // todo allow custom constructor
        .single { it.isPrimary }
        .valueParameterList
        .map { valueParameter ->
          val paramName = nameResolver.getString(valueParameter.name)

          val nullable = valueParameter.type.nullable
          val paramFqcn = valueParameter.type.extractFullName()
              .replace("`", "")
              .removeSuffix("?")

          // Get the serialized name if there is one
          // This is where we'll probably want to collect other relevant annotations (qualifiers, etc)
          // TODO it would be neat if we could read the @Transient annotation, but not sure if it makes sense vs a constructor that doesn't use it

          // This would be ideal, but unfortunately not visible right now in the kotlin-metadata library
//          val serializedName = valueParameter.getAnnotation(Json::class.java)?.name ?: paramName

          // Ugly hack around missing annotation. We go fetch it from the corresponding java constructor param
          val actualElement = ElementFilter.constructorsIn(element.enclosedElements)
              .first()
              .parameters
              .find { it.simpleName.toString() == paramName }!!
          if (paramName == "commentsCount"
              && actualElement.getAnnotation(Json::class.java) == null) {
            throw RuntimeException("Wat")
          }
          val serializedName = actualElement.getAnnotation(Json::class.java)?.name
              ?: paramName

          Parameter(
              name = paramName,
              fqClassName = paramFqcn,
              serializedName = serializedName,
              hasDefault = valueParameter.declaresDefaultValue,
              nullable = nullable,
              typeName = actualElement.asType()
                  .asTypeName()
                  .fixTypes()
                  .let { if (nullable) it.asNullable() else it })
        }


    return ClassToAdapt(
        fqClassName = fqClassName,
        packageName = packageName,
        parameterList = parameters,
        originalElement = element,
        hasCompanionObject = hasCompanionObject,
        visibility = classProto.visibility!!)
  }

  private fun errorMustBeDataClass(element: Element) {
    messager.printMessage(ERROR,
        "@${MoshiSerializable::class.java.simpleName} can't be applied to $element: must be a Kotlin data class",
        element)
  }

  private fun ClassToAdapt.generateAndWrite(): Boolean {
    val generatedDir = generatedDir ?: run {
      messager.printMessage(ERROR, "Can't find option '$kaptGeneratedOption'")
      return false
    }
    val adapterName = "${name}_JsonAdapter"
    val fileBuilder = FileSpec.builder(packageName, adapterName)
    generate(adapterName, fileBuilder)
    fileBuilder
        .build()
        .writeTo(generatedDir)
    return true
  }
}

private fun TypeName.fixTypes(): TypeName {
  // TODO Would like to make this more elegant.... ¯\_(ツ)_/¯
  // Not necessary though if we can properly read the type name off the class
  val targetType = this
  when (targetType) {
    is ClassName -> {
      return when {
        this == java.lang.String::class.java.asClassName() -> String::class.asTypeName()
        this == java.util.List::class.java.asClassName() -> List::class.asTypeName()
        this == java.util.Set::class.java.asClassName() -> Set::class.asTypeName()
        this == java.util.Map::class.java.asClassName() -> Map::class.asTypeName()
        this == java.lang.Object::class.java.asClassName() -> ANY
        this == java.lang.Void::class.java.asClassName() -> UNIT
        this == java.lang.Boolean::class.java.asClassName() -> BOOLEAN
        this == java.lang.Byte::class.java.asClassName() -> BYTE
        this == java.lang.Short::class.java.asClassName() -> SHORT
        this == java.lang.Integer::class.java.asClassName() -> INT
        this == java.lang.Long::class.java.asClassName() -> LONG
        this == java.lang.Character::class.java.asClassName() -> CHAR
        this == java.lang.Float::class.java.asClassName() -> FLOAT
        this == java.lang.Double::class.java.asClassName() -> DOUBLE
        else -> this
      }
    }
    is ParameterizedTypeName -> return ParameterizedTypeName.get(
        targetType.rawType.fixTypes() as ClassName,
        *(targetType.typeArguments.map { it.fixTypes() }.toTypedArray()))
    is WildcardTypeName -> {
      return when {
        targetType.lowerBounds.isNotEmpty() -> {
          WildcardTypeName.supertypeOf(targetType.lowerBounds[1].fixTypes())
        }
        targetType.upperBounds.isNotEmpty() -> {
          WildcardTypeName.subtypeOf(targetType.upperBounds[0].fixTypes())
        }
        else -> throw IllegalArgumentException(
            "Unrepresentable wildcard type. Cannot have more than one bound: " + targetType)
      }
    }
  }
  return this
}

private val TypeName.isPrimitive: Boolean
  get() = !nullable && this in PRIMITIVE_TYPES

private val PRIMITIVE_TYPES = setOf(
    BOOLEAN,
    BYTE,
    SHORT,
    INT,
    LONG,
    CHAR,
    FLOAT,
    DOUBLE
)

private fun primitiveDefaultFor(typeName: TypeName): String {
  return when (typeName) {
    BOOLEAN -> "false"
    BYTE -> "0 as Byte"
    SHORT -> "0 as Short"
    INT -> "0"
    LONG -> "0L"
    CHAR -> "'0'"
    FLOAT -> "0.0f"
    DOUBLE -> "0.0d"
    else -> throw IllegalArgumentException("Non-primitive type! $typeName")
  }
}

private fun TypeName.makeType(): CodeBlock {
  val targetType = this.asNonNullable()
  return when (targetType) {
    is ClassName -> CodeBlock.of("%T::class.java", targetType)
    is ParameterizedTypeName -> CodeBlock.of(
        "%T.newParameterizedType(%T::class.java, ${targetType.typeArguments
            .joinToString(", ") { "%L" }})",
        Types::class.asTypeName(),
        targetType.rawType,
        *(targetType.typeArguments.map { it.makeType() }.toTypedArray()))
    is WildcardTypeName -> {
      val target: TypeName
      val method: String
      when {
        targetType.lowerBounds.size == 1 -> {
          target = targetType.lowerBounds[0]
          method = "supertypeOf"
        }
        targetType.upperBounds.size == 1 -> {
          target = targetType.upperBounds[0]
          method = "subtypeOf"
        }
        else -> throw IllegalArgumentException(
            "Unrepresentable wildcard type. Cannot have more than one bound: " + targetType)
      }
      CodeBlock.of("%T.%L(%T::class.java)", Types::class.asTypeName(), method, target)
    }
    is TypeVariableName -> TODO()
  // Shouldn't happen
    else -> throw IllegalArgumentException("Unrepresentable type: " + targetType)
  }
}

private data class Parameter(
    val name: String,
    val fqClassName: String,
    val serializedName: String,
    val hasDefault: Boolean,
    val nullable: Boolean,
    val typeName: TypeName)

private data class ClassToAdapt(
    val fqClassName: String,
    val packageName: String,
    val parameterList: List<Parameter>,
    val originalElement: Element,
    val name: String = fqClassName.substringAfter(packageName).replace('.',
        '_').removePrefix("_"),
    val hasCompanionObject: Boolean,
    val visibility: Visibility) {
  fun generate(adapterName: String, fileSpecBuilder: FileSpec.Builder) {
    val originalTypeName = originalElement.asType().asTypeName() as ClassName
    val moshiParam = ParameterSpec.builder("moshi", Moshi::class.asClassName()).build()
    val moshiProperty = PropertySpec.builder("moshi", Moshi::class.asClassName(), PRIVATE)
        .initializer("%N", moshiParam)
        .build()
    val reader = ParameterSpec.builder("reader", JsonReader::class.asClassName()).build()
    val writer = ParameterSpec.builder("writer", JsonWriter::class.asClassName()).build()
    val value = ParameterSpec.builder("value", originalTypeName.asNullable()).build()
    val jsonAdapterTypeName = ParameterizedTypeName.get(JsonAdapter::class.asClassName(),
        originalTypeName)
    val adapter = TypeSpec.classBuilder(adapterName)
        .superclass(jsonAdapterTypeName)
        .apply {
          // TODO make this configurable. Right now it just matches the source model
          if (visibility == INTERNAL) {
            addModifiers(KModifier.INTERNAL)
          }
        }
        .addProperty(moshiProperty)
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(moshiParam)
            .build())
        .addFunction(FunSpec.builder("fromJson")
            .addModifiers(OVERRIDE)
            .addParameter(reader)
            .returns(originalTypeName.asNullable())
            .beginControlFlow("if (%N.peek() == %T.NULL)", reader,
                JsonReader.Token.NULL.declaringClass.asTypeName())
            .addStatement("%N.nextNull<%T>()", reader, ANY)
            .endControlFlow()
            .apply {
              parameterList.forEach { param ->
                if (param.nullable) {
                  addStatement("var ${param.name}: %T = null", param.typeName)
                } else {
                  if (param.typeName.isPrimitive) {
                    addStatement("var ${param.name} = %L",
                        primitiveDefaultFor(param.typeName))
                  } else {
                    addStatement("lateinit var ${param.name}: %T", param.typeName)
                  }
                }
              }
            }
            .addStatement("%N.beginObject()", reader)
            // TODO Use select() API for optimization
            .beginControlFlow("while (%N.hasNext())", reader)
            .beginControlFlow("when (%N.nextName())", reader)
            .apply {
              parameterList.forEach { param ->
                // TODO we should probably track which ones are required and haven't been set instead of using lateinit
                val possibleBangs = if (param.nullable) "" else "!!"
                addStatement("%S -> %L = %N.adapter%L(%L).fromJson(%N)$possibleBangs",
                    param.serializedName,
                    param.name,
                    moshiParam,
                    if (param.typeName is ClassName) "" else CodeBlock.of("<%T>", param.typeName),
                    param.typeName.makeType(),
                    reader)
              }
            }
            .addStatement("else -> %N.skipValue()", reader)
            .endControlFlow()
            .endControlFlow()
            .addStatement("%N.endObject()", reader)
            // TODO How can we skip params with default values without doing all permutations?
            // TODO one idea - maybe we can read default value initializers and inline them somehow to the placeholder fields
            .addStatement("return %T(%L)",
                originalTypeName,
                parameterList.joinToString(",\n") { "${it.name} = ${it.name}" })
            .build())
        .addFunction(FunSpec.builder("toJson")
            .addModifiers(OVERRIDE)
            .addParameter(writer)
            .addParameter(value)
            .beginControlFlow("if (%N == null)", value)
            .addStatement("%N.nullValue()", writer)
            .addStatement("return")
            .endControlFlow()
            .addStatement("%N.beginObject()", writer)
            .apply {
              parameterList.forEach { param ->
                if (param.nullable) {
                  beginControlFlow("if (%N.%L != null)", value, param.name)
                }
                addStatement("%N.name(%S)", writer, param.serializedName)
                addStatement("%N.adapter%L(%L).toJson(%N, %N.%L)",
                    moshiParam,
                    if (param.typeName is ClassName) "" else CodeBlock.of("<%T>", param.typeName),
                    param.typeName.makeType(),
                    writer,
                    value,
                    param.name)
                if (param.nullable) {
                  endControlFlow()
                }
              }
            }
            .addStatement("%N.endObject()", writer)
            .build())
        .build()

    if (hasCompanionObject) {
      fileSpecBuilder.addFunction(FunSpec.builder("jsonAdapter")
          .apply {
            // TODO make this configurable. Right now it just matches the source model
            if (visibility == INTERNAL) {
              addModifiers(KModifier.INTERNAL)
            }
          }
          .receiver(originalTypeName.nestedClass("Companion"))
          .returns(jsonAdapterTypeName)
          .addParameter(moshiParam)
          .addStatement("return %N(%N)", adapter, moshiParam)
          .build())
    }
    fileSpecBuilder.addType(adapter)
  }
}
