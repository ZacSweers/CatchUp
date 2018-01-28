package io.sweers.moshkt.compiler

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
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
import com.squareup.kotlinpoet.KModifier.IN
import com.squareup.kotlinpoet.KModifier.OUT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
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
import org.jetbrains.kotlin.serialization.ProtoBuf.Type.Argument.Projection
import org.jetbrains.kotlin.serialization.ProtoBuf.TypeParameter.Variance
import org.jetbrains.kotlin.serialization.ProtoBuf.Visibility
import org.jetbrains.kotlin.serialization.ProtoBuf.Visibility.INTERNAL
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
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
 */
@AutoService(Processor::class)
class MoshKtProcessor : KotlinAbstractProcessor(), KotlinMetadataUtils {

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

  private fun processElement(element: Element): Adapter? {
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
          val serializedName = actualElement.getAnnotation(Json::class.java)?.name
              ?: paramName

          Property(
              name = paramName,
              fqClassName = paramFqcn,
              serializedName = serializedName,
              hasDefault = valueParameter.declaresDefaultValue,
              nullable = nullable,
              typeName = valueParameter.type.asTypeName(nameResolver, classProto::getTypeParameter))
        }


    return Adapter(
        fqClassName = fqClassName,
        packageName = packageName,
        propertyList = parameters,
        originalElement = element,
        hasCompanionObject = hasCompanionObject,
        visibility = classProto.visibility!!,
        elementUtils = elementUtils)
  }

  private fun errorMustBeDataClass(element: Element) {
    messager.printMessage(ERROR,
        "@${MoshiSerializable::class.java.simpleName} can't be applied to $element: must be a Kotlin data class",
        element)
  }

  private fun Adapter.generateAndWrite(): Boolean {
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

/**
 * Creates a joined string representation of simplified typename names.
 */
private fun List<TypeName>.simplifiedNames(): String {
  return joinToString("_") { it.simplifiedName() }
}

/**
 * Creates a simplified string representation of a TypeName's name
 */
private fun TypeName.simplifiedName(): String {
  return when (this) {
    is ClassName -> simpleName().decapitalize()
    is ParameterizedTypeName -> {
      rawType.simpleName().decapitalize() + if (typeArguments.isEmpty()) "" else "__" + typeArguments.simplifiedNames()
    }
    is WildcardTypeName -> "wildcard__" + (lowerBounds + upperBounds).simplifiedNames()
    is TypeVariableName -> name.decapitalize() + if (bounds.isEmpty()) "" else "__" + bounds.simplifiedNames()
  // Shouldn't happen
    else -> toString().decapitalize()
  }
}

private fun ClassName.isClass(elementUtils: Elements): Boolean {
  val fqcn = toString()
  if (fqcn.startsWith("kotlin.collections.")) {
    // These are special kotlin interfaces are only visible in kotlin, because they're replaced by
    // the compiler with concrete java classes
    return false
  } else if (this == ARRAY) {
    // This is a "fake" class and not visible to Elements
    return true
  }
  return elementUtils.getTypeElement(fqcn).kind == ElementKind.INTERFACE
}

private fun TypeName.makeType(elementUtils: Elements): CodeBlock {
  if (nullable) {
    return asNonNullable().makeType(elementUtils)
  }
  return when (this) {
    is ClassName -> CodeBlock.of("%T::class.java", this)
    is ParameterizedTypeName -> {
      // If it's a Class type, we have to specify the generics.
      val rawTypeParameters = if (rawType.isClass(elementUtils)) {
        CodeBlock.of(
            typeArguments.joinTo(
                buffer = StringBuilder(),
                separator = ", ",
                prefix = "<",
                postfix = ">") { "%T" }
                .toString(),
            *(typeArguments.toTypedArray())
        )
      } else {
        CodeBlock.of("")
      }
      CodeBlock.of(
          "%T.newParameterizedType(%T%L::class.java, ${typeArguments
              .joinToString(", ") { "%L" }})",
          Types::class.asTypeName(),
          rawType,
          rawTypeParameters,
          *(typeArguments.map { it.makeType(elementUtils) }.toTypedArray()))
    }
    is WildcardTypeName -> {
      val target: TypeName
      val method: String
      when {
        lowerBounds.size == 1 -> {
          target = lowerBounds[0]
          method = "supertypeOf"
        }
        upperBounds.size == 1 -> {
          target = upperBounds[0]
          method = "subtypeOf"
        }
        else -> throw IllegalArgumentException(
            "Unrepresentable wildcard type. Cannot have more than one bound: " + this)
      }
      CodeBlock.of("%T.%L(%T::class.java)", Types::class.asTypeName(), method, target)
    }
    is TypeVariableName -> TODO()
  // Shouldn't happen
    else -> throw IllegalArgumentException("Unrepresentable type: " + this)
  }
}

private data class Property(
    val name: String,
    val fqClassName: String,
    val serializedName: String,
    val hasDefault: Boolean,
    val nullable: Boolean,
    val typeName: TypeName)

private data class Adapter(
    val fqClassName: String,
    val packageName: String,
    val propertyList: List<Property>,
    val originalElement: Element,
    val name: String = fqClassName.substringAfter(packageName)
        .replace('.', '_')
        .removePrefix("_"),
    val hasCompanionObject: Boolean,
    val visibility: Visibility,
    val elementUtils: Elements) {
  fun generate(adapterName: String, fileSpecBuilder: FileSpec.Builder) {
    val nameAllocator = NameAllocator()
    fun String.allocate() = nameAllocator.newName(this)

    val originalTypeName = originalElement.asType().asTypeName() as ClassName
    val moshiName = "moshi".allocate()
    val moshiParam = ParameterSpec.builder(moshiName, Moshi::class.asClassName()).build()
    val reader = ParameterSpec.builder("reader".allocate(),
        JsonReader::class.asClassName()).build()
    val writer = ParameterSpec.builder("writer".allocate(),
        JsonWriter::class.asClassName()).build()
    val value = ParameterSpec.builder("value".allocate(),
        originalTypeName.asNullable()).build()
    val jsonAdapterTypeName = ParameterizedTypeName.get(JsonAdapter::class.asClassName(),
        originalTypeName)

    // Create fields
    val adapterProperties = propertyList
        .map { it.typeName }
        .distinct()
        .associate { typeName ->
          val propertyName = "${typeName.simplifiedName().allocate()}_Adapter"
          val adapterTypeName = ParameterizedTypeName.get(JsonAdapter::class.asTypeName(), typeName)
          typeName to PropertySpec.builder(propertyName, adapterTypeName, PRIVATE)
              .initializer("%N.adapter%L(%L)",
                  moshiParam,
                  if (typeName is ClassName) "" else CodeBlock.of("<%T>", typeName),
                  typeName.makeType(elementUtils))
              .build()
        }

    // TODO in the fiture make these propertyspecs directly. Not reasonable right now because
    // kotlinpoet only toString()'s it
    val allocatedNames = propertyList.associate { it to it.name.allocate() }

    val adapter = TypeSpec.classBuilder(adapterName)
        .superclass(jsonAdapterTypeName)
        .apply {
          // TODO make this configurable. Right now it just matches the source model
          if (visibility == INTERNAL) {
            addModifiers(KModifier.INTERNAL)
          }
        }
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter(moshiParam)
            .build())
        .addProperties(adapterProperties.values)
        .addFunction(FunSpec.builder("fromJson")
            .addModifiers(OVERRIDE)
            .addParameter(reader)
            .returns(originalTypeName.asNullable())
            .beginControlFlow("if (%N.peek() == %T.NULL)", reader,
                JsonReader.Token.NULL.declaringClass.asTypeName())
            .addStatement("%N.nextNull<%T>()", reader, ANY)
            .endControlFlow()
            .apply {
              propertyList.forEach { prop ->
                when {
                  prop.nullable -> {
                    addStatement("var ${allocatedNames[prop]}: %T = null", prop.typeName)
                  }
                  prop.hasDefault -> {
                    addStatement("var ${allocatedNames[prop]}: %T = null",
                        prop.typeName.asNullable())
                  }
                  prop.typeName.isPrimitive -> {
                    addStatement("var ${allocatedNames[prop]} = %L",
                        primitiveDefaultFor(prop.typeName))
                  }
                  else -> addStatement("lateinit var ${allocatedNames[prop]}: %T", prop.typeName)
                }
              }
            }
            .addStatement("%N.beginObject()", reader)
            // TODO Use select() API for optimization
            .beginControlFlow("while (%N.hasNext())", reader)
            .beginControlFlow("when (%N.nextName())", reader)
            .apply {
              propertyList.forEach { prop ->
                val possibleBangs = if (prop.nullable) "" else "!!"
                addStatement("%S -> %L = %N.fromJson(%N)$possibleBangs",
                    prop.serializedName,
                    prop.name,
                    adapterProperties[prop.typeName]!!,
                    reader)
              }
            }
            .addStatement("else -> %N.skipValue()", reader)
            .endControlFlow()
            .endControlFlow()
            .addStatement("%N.endObject()", reader)
            .apply {
              val propertiesWithDefaults = propertyList.filter { it.hasDefault }
              if (propertiesWithDefaults.isEmpty()) {
                addStatement("return %T(%L)",
                    originalTypeName,
                    propertyList.joinToString(",\n") { "${it.name} = ${allocatedNames[it]}" })
              } else {
                addStatement("return %T(%L).let {\n  it.copy(%L)\n}",
                    originalTypeName,
                    propertyList
                        .filter { !it.hasDefault }
                        .joinToString(",\n") { "${it.name} = ${allocatedNames[it]}" },
                    propertiesWithDefaults
                        .joinToString(",\n      ") {
                          "${it.name} = ${allocatedNames[it]} ?: it.${it.name}"
                        })
              }
            }
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
              propertyList.forEach { prop ->
                if (prop.nullable) {
                  beginControlFlow("if (%N.%L != null)", value, prop.name)
                }
                addStatement("%N.name(%S)", writer, prop.serializedName)
                addStatement("%N.toJson(%N, %N.%L)",
                    adapterProperties[prop.typeName]!!,
                    writer,
                    value,
                    prop.name)
                if (prop.nullable) {
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

private fun ProtoBuf.TypeParameter.asTypeName(
    nameResolver: NameResolver,
    getTypeParameter: (index: Int) -> ProtoBuf.TypeParameter): TypeName {
  return TypeVariableName(
      name = nameResolver.getString(name),
      bounds = *(upperBoundList.map { it.asTypeName(nameResolver, getTypeParameter) }
          .toTypedArray()),
      variance = variance.asKModifier()
  )
}

private fun ProtoBuf.TypeParameter.Variance.asKModifier(): KModifier {
  return when (this) {
    Variance.IN -> IN
    Variance.OUT -> OUT
    Variance.INV -> TODO("INV variance is unsupported")
  }
}

/**
 * Returns the TypeName of this type as it would be seen in the source code,
 * including nullability and generic type parameters.
 *
 * @param [nameResolver] a [NameResolver] instance from the source proto
 * @param [getTypeParameter]
 * A function that returns the type parameter for the given index.
 * **Only called if [ProtoBuf.Type.hasTypeParameter] is `true`!**
 */
private fun ProtoBuf.Type.asTypeName(
    nameResolver: NameResolver,
    getTypeParameter: (index: Int) -> ProtoBuf.TypeParameter
): TypeName {

  val argumentList = when {
    hasAbbreviatedType() -> abbreviatedType.argumentList
    else -> argumentList
  }

  if (hasFlexibleUpperBound()) {
    return WildcardTypeName.subtypeOf(
        flexibleUpperBound.asTypeName(nameResolver, getTypeParameter))
  } else if (hasOuterType()) {
    return WildcardTypeName.supertypeOf(outerType.asTypeName(nameResolver, getTypeParameter))
  }

  val realType = when {
    hasTypeParameter() -> return getTypeParameter(typeParameter)
        .asTypeName(nameResolver, getTypeParameter)
    hasTypeParameterName() -> typeParameterName
    hasAbbreviatedType() -> abbreviatedType.typeAliasName
    else -> className
  }

  var typeName: TypeName = ClassName.bestGuess(nameResolver.getString(realType)
      .replace("/", "."))

  if (argumentList.isNotEmpty()) {
    val remappedArgs: Array<TypeName> = argumentList.map {
      val projection = if (it.hasProjection()) {
        it.projection
      } else null
      if (it.hasType()) {
        it.type.asTypeName(nameResolver, getTypeParameter)
            .let { typeName ->
              projection?.let {
                when (it) {
                  Projection.IN -> WildcardTypeName.supertypeOf(typeName)
                  Projection.OUT -> {
                    if (typeName == ANY) {
                      // This becomes a *, which we actually don't want here.
                      // List<Any> works with List<*>, but List<*> doesn't work with List<Any>
                      typeName
                    } else {
                      WildcardTypeName.subtypeOf(typeName)
                    }
                  }
                  Projection.STAR -> WildcardTypeName.subtypeOf(ANY)
                  Projection.INV -> TODO("INV projection is unsupported")
                }
              } ?: typeName
            }
      } else {
        WildcardTypeName.subtypeOf(ANY)
      }
    }.toTypedArray()
    typeName = ParameterizedTypeName.get(typeName as ClassName, *remappedArgs)
  }

  if (nullable) {
    typeName = typeName.asNullable()
  }

  return typeName
}
