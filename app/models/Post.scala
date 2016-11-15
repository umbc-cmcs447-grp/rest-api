package models

case class Post(postId: String,
                authorId: String,
                title: String,
                body: String,
                category: String,
                status: String,
                created: Long,
                lastModified: Long)
