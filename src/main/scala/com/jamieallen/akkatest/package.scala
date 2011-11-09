package com.jamieallen.akkatest

package object impl {
	sealed trait ProducerMessage
  case object GetUpdates extends ProducerMessage

  sealed trait ConsumerMessage
	case object TakeNextFromQueue extends ConsumerMessage
}