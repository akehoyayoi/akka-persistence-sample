//#full-example
package com.lightbend.akka.sample

import akka.actor.{ActorSystem, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}

private sealed trait MyCommand
private case object MySnapshot extends MyCommand
private case object MyPrint extends MyCommand
private case class AppendCommand(data: Int) extends MyCommand

// 内部状態
private case class MyState(events: Seq[Int] = Seq.empty) {
  def updated(evt: AppendCommand) = copy(evt.data +: events)
  def state = toString
  override def toString: String = events.reverse.mkString(" :: ")
}


class ExamplePersistentActor extends PersistentActor {
  // メッセージを永続化する際のID
  override def persistenceId = "example-id"

  // 内部状態
  private var state = MyState()

  // 状態を復元する際に実行される
  override def receiveRecover: Receive = {
    case command: AppendCommand =>
      // メッセージからの復元
      state = state.updated(command)
    case SnapshotOffer(_, snapshot: MyState) =>
      // Snapshotからの復元
      state = snapshot
  }

  // Actorのreceiveにあたるもの。何かしらのcommandに対する処理
  override def receiveCommand: Receive = {
    case command: AppendCommand  =>
      // メッセージを永続化している
      persist(command) { _command =>
        state = state.updated(_command)
      }
    case MySnapshot => saveSnapshot(state)
    case MyPrint    => println(state.state)
  }
}


object PersistentActorExample extends App {

  val system = ActorSystem("PersistentActorExample")
  val persistentActor = system.actorOf(Props[ExamplePersistentActor], "my-example")

  // send messages to target actor
  persistentActor ! AppendCommand(-1)
 // persistentActor ! MySnapshot
  persistentActor ! AppendCommand(3)
  persistentActor ! MyPrint

  Thread.sleep(1000)
  system.terminate()
}