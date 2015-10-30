/*
 * Copyright 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.twitter.zipkin.common

import java.net.{InetAddress, InetSocketAddress}
import java.nio.ByteBuffer

import com.google.common.collect.ComparisonChain

import scala.util.hashing.MurmurHash3

/**
 * Represents the client or server machine we traced.
 *
 * @param ipv4 ipv4 ip address.
 * @param port note that due to lack of unsigned integers this will wrap.
 * @param _serviceName the service this operation happened on, in lowercase
 */
// This is not a case-class as we need to enforce serviceName as lowercase
class Endpoint(val ipv4: Int, val port: Short, _serviceName: String) extends Ordered[Endpoint] {

  /**  the service this operation happened on, in lowercase. */
  val serviceName: String = _serviceName.toLowerCase

  override lazy val toString = "%s:%d(%s)".format(getHostAddress, port, serviceName)

  /**
   * Return the java.net.InetSocketAddress which contains host/port
   */
  def getInetSocketAddress: InetSocketAddress = {
    val bytes = ByteBuffer.allocate(4).putInt(this.ipv4).array()
    new InetSocketAddress(InetAddress.getByAddress(bytes), this.getUnsignedPort)
  }

  /**
   * Convenience function to get the string-based ip-address
   */
  def getHostAddress: String = {
    "%d.%d.%d.%d".format(
      (ipv4 >> 24) & 0xFF,
      (ipv4 >> 16) & 0xFF,
      (ipv4 >> 8) & 0xFF,
      ipv4 & 0xFF)
  }

  def getUnsignedPort: Int = {
    port & 0xFFFF
  }

  override def compare(that: Endpoint) = ComparisonChain.start()
    .compare(serviceName, that.serviceName)
    .compare(ipv4, that.ipv4)
    .compare(port, that.port)
    .result()

  override lazy val hashCode: Int = MurmurHash3.seqHash(List(serviceName, ipv4, port))

  override def equals(other: Any) = other match {
    case that: Endpoint =>
      this.serviceName == that.serviceName && this.ipv4 == that.ipv4 && this.port == that.port
    case _ => false
  }

  def copy(ipv4: Int = this.ipv4, port: Short = this.port, serviceName: String = this.serviceName) =
    Endpoint(ipv4, port, serviceName)
}

object Endpoint {
  val Unknown = Endpoint(0, 0, "")
  val UnknownServiceName = "Unknown service name"

  def apply(ipv4: Int, port: Short, serviceName: String) = new Endpoint(ipv4, port, serviceName)
}