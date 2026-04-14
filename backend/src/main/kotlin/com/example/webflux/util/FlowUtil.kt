package com.example.webflux.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.fold

suspend fun <K, V> Flow<V>.groupBy(keySelector: (V) -> K,
                                   capacity: Int,
                                   listCapacity: Int = 16): Map<K, List<V>> =
    fold(HashMap<K, ArrayList<V>>(capacity, 1f)) { acc, value ->
        acc.apply {
            getOrPut(keySelector(value)) { ArrayList(listCapacity) }.add(value)
        }
    }

suspend fun <K, V> Flow<V>.groupBy(keySelector: suspend (V) -> K,
                                   capacity: Int,
                                   listCapacity: Int = 16): Map<K, List<V>> =
    fold(HashMap<K, ArrayList<V>>(capacity, 1f)) { acc, value ->
        acc.apply {
            getOrPut(keySelector(value)) { ArrayList(listCapacity) }.add(value)
        }
    }

suspend fun <T> FlowCollector<T>.emitAll(elements: Iterable<T>) =
    elements.forEach { emit(it) }

suspend fun <K, V> Flow<V>.associateBy(keySelector: suspend (V) -> K): Map<K, V> =
    fold(HashMap()) { acc, value -> acc.also { acc[keySelector(value)] = value } }
