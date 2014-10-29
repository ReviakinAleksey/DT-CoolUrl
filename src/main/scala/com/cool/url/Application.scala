package com.cool.url

import com.cool.url.config.Environment


object Application extends App {
  new Environment("production") {

    try {
      webService.start()
    } finally {
      connector.shutdown()
    }
  }
}
