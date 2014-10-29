package com.cool.url

import java.io.PrintWriter

import com.cool.url.config.Environment
import com.cool.url.model.SchemaComponent


object DdlGenerator extends App{
  val context = new Environment("production") with SchemaComponent

  val createSql = new PrintWriter("create.sql")
  context.ddl.createStatements.foreach(createSql.println)
  createSql.close()

  val dropSql = new PrintWriter("close.sql")
  context.ddl.dropStatements.foreach(dropSql.println)
  dropSql.close()
}
