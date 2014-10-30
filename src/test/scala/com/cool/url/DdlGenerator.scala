package com.cool.url

import java.io.PrintWriter

import com.cool.url.config.Environment
import com.cool.url.model.SchemaComponent


object DdlGenerator extends App{
  val context = new Environment("production") with SchemaComponent

  val createSql = new PrintWriter("create.sql")
  context.ddl.createStatements.foreach(sql => createSql.println(sql+";"))
  createSql.close()

  val dropSql = new PrintWriter("drop.sql")
  context.ddl.dropStatements.foreach(sql => dropSql.println(sql+";"))
  dropSql.close()
}
