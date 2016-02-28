package com.crossbowffs.waifupaper.loader

import jp.live2d.util.Json

@Suppress("UNCHECKED_CAST")
fun Json.Value.toList() = this.getVector(null) as List<Json.Value>

@Suppress("UNCHECKED_CAST")
fun Json.Value.toMap() = this.getMap(null) as Map<String, Json.Value>

fun Json.Value.getAsString(key: String) = this.get(key)?.toString()

fun Json.Value.getAsList(key: String) = this.get(key)?.toList()

fun Json.Value.getAsMap(key: String) = this.get(key)?.toMap()
