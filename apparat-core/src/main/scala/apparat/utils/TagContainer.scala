/*
 * This file is part of Apparat.
 *
 * Copyright (C) 2010 Joa Ebert
 * http://www.joa-ebert.com/
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package apparat.utils

import apparat.swc._
import apparat.swf._
import apparat.utils.IO._
import java.io.{
	FileInputStream => JFileInputStream,
	FileOutputStream => JFileOutputStream,
	BufferedInputStream => JBufferedInputStream,
	File => JFile,
	InputStream => JInputStream,
	OutputStream => JOutputStream
}

object TagContainer {
	def fromFile(pathname: String): TagContainer = fromFile(new JFile(pathname))

	def fromFile(file: JFile): TagContainer = {
		val tc = new TagContainer
		tc read file
		tc
	}
}

class TagContainer extends SwfTagMapping {
	var strategy: Option[TagContainerStrategy] = None

	def tags: List[SwfTag] = strategy match {
		case Some(x) => x.tags
		case None => List()
	}

	def tags_=(value: List[SwfTag]) = strategy match {
		case Some(x) => x.tags = value
		case None => {}
	}

	private def strategyFor(file: JFile) = file.getName.toLowerCase match {
		case x if x endsWith ".swf" => Some(new SwfStrategy)
		case x if x endsWith ".swc" => Some(new SwcStrategy)
		case x => {
			using(new JFileInputStream(file)) {
				input => {
					val b0 = input.read()

					if(('F' == b0 || 'C' == b0) && 'W' == input.read() && 'S' == input.read()) {
						Some(new SwfStrategy)
					} else if ('P' == b0 && 'K' == input.read()) {
						Some(new SwcStrategy)
					} else {
						None
					}
				}
			}
		}
	}

	def read(pathname: String): Unit = read(new JFile(pathname))

	def read(file: JFile): Unit = {
		strategy = strategyFor(file)
		strategy match {
			case Some(x) => {
				using(new JBufferedInputStream(new JFileInputStream(file), 0x8000))(input => {
					x read (input, file length)
				})
			}
			case None =>
		}
	}

	def write(pathname: String): Unit = write(new JFile(pathname))

	def write(file: JFile): Unit = {
		strategy match {
			case Some(x) => {
				using(new JFileOutputStream(file))(output => {
					x write output
				})
			}
			case None =>
		}
	}

	def write(output: JOutputStream): Unit = strategy match {
		case Some(x) => x write output
		case None =>
	}
}

trait TagContainerStrategy {
	def read(input: JInputStream, length: Long)

	def write(output: JOutputStream)

	def tags: List[SwfTag]

	def tags_=(value: List[SwfTag])
}

class SwfStrategy extends TagContainerStrategy {
	var swf: Option[Swf] = None

	override def tags: List[SwfTag] = swf match {
		case Some(x) => x.tags
		case None => Nil
	}

	override def tags_=(value: List[SwfTag]) = swf match {
		case Some(x) => x.tags = value
		case None =>
	}

	override def read(input: JInputStream, length: Long) = {
		swf = Some(Swf fromInputStream (input, length))
	}

	override def write(output: JOutputStream) = {
		swf match {
			case Some(x) => x write output
			case None =>
		}
	}
}

class SwcStrategy extends TagContainerStrategy {
	var swc: Option[Swc] = None
	var swf: Option[Swf] = None

	override def tags: List[SwfTag] = swf match {
		case Some(x) => x.tags
		case None => Nil
	}

	override def tags_=(value: List[SwfTag]) = swf match {
		case Some(x) => x.tags = value
		case None =>
	}

	override def read(input: JInputStream, length: Long) = {
		swc = Some(Swc fromInputStream input)
		swf = Some(Swf fromSwc swc.getOrElse(error("Could not read SWC.")))
	}

	override def write(output: JOutputStream) = {
		swc match {
			case Some(x) => {
				swf match {
					case Some(y) => {
						y write x
					}
					case None => {}
				}
				x write output
			}
			case None => {}
		}
	}
}
