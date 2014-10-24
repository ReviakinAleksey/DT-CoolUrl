package com.cool.url.model

import com.cool.url.service._


trait ServiceComponent extends UsersComponent
  with FoldersComponent
  with LinksComponent
  with ClicksComponent {
  self: DbConnectorComponent with TokenServiceComponent with QueryExtensions =>
}

trait SchemaComponent {
  this: DbConnectorComponent
    with ServiceComponent =>

  import connector.driver.simple._

  def ddl = users.ddl ++ links.ddl ++ links.codeSequenceDDL ++ folders.ddl ++ clicks.ddl

}

