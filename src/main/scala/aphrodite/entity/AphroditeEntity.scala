package aphrodite.entity

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.http.scaladsl.model.DateTime
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import akka.serialization.jackson.CborSerializable

import scala.concurrent.duration._

object AphroditeEntity {

  final case class Image(
    imageId: String,
    filename: String,
    sha512: String,
    deleted: Option[DateTime],
  ) extends CborSerializable {
    def toSummary: Summary = Summary(imageId = imageId, filename = filename, deleted = isDeleted, sha512 = sha512)

    def deleted(now: DateTime) = copy(deleted = Some(now))

    def isDeleted: Boolean = deleted.isDefined
  }

  object Image {
    val empty = Image(
      imageId = null,
      filename = "",
      deleted = None,
      sha512 = "",
    )
  }

  sealed trait Command extends CborSerializable

  final case class Summary(imageId: String, filename: String, sha512: String, deleted: Boolean) extends CborSerializable

  final case class AddImage(filename: String, sha512: String, replyTo: ActorRef[StatusReply[Summary]]) extends Command

  final case class RemoveImage(imageId: String, replyTo: ActorRef[StatusReply[Summary]]) extends Command

  final case class GetImage(replyTo: ActorRef[StatusReply[Summary]]) extends Command

  sealed trait Event extends CborSerializable

  final case class ImageAdded(imageId: String, filename: String, sha512: String) extends Event

  final case class ImageRemoved(ImageId: String, eventTime: DateTime) extends Event

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("AphroditeService")

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey)(entityContext =>
      AphroditeEntity(entityContext.entityId)))
  }

  def apply(imageId: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, Image](
      persistenceId = PersistenceId(EntityKey.name, imageId),
      emptyState = Image.empty,
      commandHandler = (state, command) => handleCommand(imageId, state, command),
      eventHandler = (state, event) => handleEvent(state, event))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1)
      )

  private def handleCommand(
                             imageId: String,
                             state: Image,
                             command: Command
  ): ReplyEffect[Event, Image] = {
    command match {
      case AddImage(filename, sha512, replyTo) =>
        Effect
          .persist(ImageAdded(imageId, filename, sha512))
          .thenReply(replyTo) { newImage =>
            StatusReply.Success(newImage.toSummary)
          }
      case RemoveImage(imageId, replyTo) =>
        Effect
          .persist(ImageRemoved(imageId, DateTime.now))
          .thenReply(replyTo) { removedImaged =>
            StatusReply.Success(removedImaged.toSummary)
          }
      case GetImage(replyTo) =>
        Effect
          .reply(replyTo)(StatusReply.Success(state.toSummary))

    }
  }

  private def handleEvent(state: Image, event: Event): Image = {
    event match {
      case ImageAdded(imageId, filename, sha512) =>
        Image(imageId, filename, sha512, None)
      case ImageRemoved(_, eventTime) =>
        state.deleted(eventTime)
    }
  }
}