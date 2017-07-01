//#full-example
package com.lightbend.akka.sample

import akka.actor.{ActorSystem, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import org.joda.time.LocalDateTime

private sealed trait MyCommand
private case object MySnapshot extends MyCommand
private case object MyPrint extends MyCommand
private case class CountUpCommand() extends MyCommand

private case class MyStatus(
  first: LocalDateTime = LocalDateTime.now(),
  last: LocalDateTime = LocalDateTime.now(),
  count: Int = 0) {
  def countUp = copy(first, LocalDateTime.now(), count + 1)
  def status = toString
  override def toString: String = first.toString() + "\n" + last.toString() + "\n" + count.toString
}

class ExamplePersistentActor extends PersistentActor {
  // メッセージを永続化する際のID
  override def persistenceId = "example-id"

  // 内部状態
  private var status = MyStatus()

  // 状態を復元する際に実行される
  override def receiveRecover: Receive = {
    case _: CountUpCommand =>
      // メッセージからの復元
      status = status.countUp
    case SnapshotOffer(_, snapshot: MyStatus) =>
      // Snapshotからの復元
      status = snapshot
  }

  // Actorのreceiveにあたるもの。何かしらのcommandに対する処理
  override def receiveCommand: Receive = {
    case command: CountUpCommand  =>
      // メッセージを永続化している
      persist(command) { _ =>
        status = status.countUp
        // Snapshotは毎回やるのはOverKillだがこれをやらないと時間が永続化できない
        saveSnapshot(status)
      }
    case MySnapshot => saveSnapshot(status)
    case MyPrint    => println(status.status)
  }
}


object PersistentActorExample extends App {

  val system = ActorSystem("PersistentActorExample")
  val persistentActor = system.actorOf(Props[ExamplePersistentActor], "my-example")

  // send messages to target actor
  persistentActor ! CountUpCommand()
  persistentActor ! CountUpCommand()
//  persistentActor ! MySnapshot
  persistentActor ! MyPrint

  Thread.sleep(1000)
  system.terminate()
}