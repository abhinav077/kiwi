package com.abhinavsirohi.kiwi.domain.usecase

fun interface UseCase<in Parameters, out Result> {
    suspend operator fun invoke(parameters: Parameters): Result
}
