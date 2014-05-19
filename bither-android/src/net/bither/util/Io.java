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

package net.bither.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Method;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Io {
	private static final Logger log = LoggerFactory.getLogger(Io.class);

	public static final long copy(@Nonnull final Reader reader,
			@Nonnull final StringBuilder builder) throws IOException {
		final char[] buffer = new char[256];
		long count = 0;
		int n = 0;
		while (-1 != (n = reader.read(buffer))) {
			builder.append(buffer, 0, n);
			count += n;
		}
		return count;
	}

	public static final long copy(@Nonnull final InputStream is,
			@Nonnull final OutputStream os) throws IOException {
		final byte[] buffer = new byte[1024];
		long count = 0;
		int n = 0;
		while (-1 != (n = is.read(buffer))) {
			os.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void chmod(@Nonnull final File path, final int mode) {
		try {
			final Class fileUtils = Class.forName("android.os.FileUtils");
			final Method setPermissions = fileUtils.getMethod("setPermissions",
					String.class, int.class, int.class, int.class);
			setPermissions.invoke(null, path.getAbsolutePath(), mode, -1, -1);
		} catch (final Exception x) {
			log.info("problem using undocumented chmod api", x);
		}
	}
}
