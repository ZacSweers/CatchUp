package catchup.di

fun interface ContextualFactory<Input, Output> {
  fun create(input: Input): Output
}
