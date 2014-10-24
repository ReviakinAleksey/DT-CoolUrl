package com.cool.url.service

import scala.slick.lifted.ProvenShape


trait FoldersComponent {
  this: DbConnectorComponent
    with UsersComponent =>

  import connector.driver.simple._

  case class Folder(id: Long, token: UserToken, title: String)

  class Folders(tag: Tag) extends Table[Folder](tag, connector.schema, "folders") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def token = column[UserToken]("user_token")

    def title = column[String]("title")

    override def * : ProvenShape[Folder] = (id, token, title) <> (Folder.tupled, Folder.unapply)

    def user = foreignKey("fK_folders_to_user", token, users)(_.token, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)


    def id_to_token_index = index("idx_folders_id_and_token" ,(id, token), unique = true)
  }

  object folders extends TableQuery(new Folders(_))

}