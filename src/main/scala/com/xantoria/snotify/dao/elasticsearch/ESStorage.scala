package com.xantoria.snotify.dao.elasticsearch

import com.sksamuel.elastic4s.http.HttpClient

class ESStorage(
  override protected val client: HttpClient,
  override protected val indexName: String
) extends ESHandling
