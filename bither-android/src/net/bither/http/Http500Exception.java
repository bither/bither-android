/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.http;

public class Http500Exception extends HttpException {
	private static final long serialVersionUID = 1L;

	public Http500Exception(Exception cause) {
		super(cause);
	}

	public Http500Exception(String msg, int statusCode) {
		super(msg, statusCode);
	}

	public Http500Exception(String msg, Exception cause, int statusCode) {
		super(msg, cause, statusCode);

	}

	public Http500Exception(String msg, Exception cause) {
		super(msg, cause);
	}

	public Http500Exception(String msg) {
		super(msg);
	}
}
