// Copyright (C) 2015 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

public class TokenReplaceOutputStream extends ServletOutputStream {

  private final byte[] token;

  private final byte[] replace;

  private final ByteArrayOutputStream outBuff;

  private final HttpServletResponse resp;

  private final int outLen;

  public TokenReplaceOutputStream(HttpServletResponse resp, int contentLength,
      byte[] token, byte[] replace) {
    this.resp = resp;
    this.outLen = contentLength;
    this.token = token;
    this.replace = replace;
    this.outBuff = new ByteArrayOutputStream(contentLength);
  }

  @Override
  public void write(int b) throws IOException {
    outBuff.write(b);
  }

  @Override
  public void flush() throws IOException {
    if (outBuff.size() < outLen) {
      return;
    }

    byte[] outData = outBuff.toByteArray();
    byte[] cmp = new byte[token.length];
    ByteArrayOutputStream convertedData =
        new ByteArrayOutputStream(outData.length);

    for (int i = 0; i < outData.length; i++) {
      byte b = outData[i];
      if (b != token[0] || (outData.length - i) < token.length) {
        convertedData.write(outData, i, 1);
        continue;
      }

      System.arraycopy(outData, i, cmp, 0, token.length);
      if (Arrays.equals(cmp, token)) {
        convertedData.write(replace);
        i += token.length - 1;
      }
    }

    resp.setHeader("Content-Length", "" + convertedData.size());

    ServletOutputStream out = resp.getOutputStream();
    out.write(convertedData.toByteArray());
    out.flush();
  }

  @Override
  public void close() throws IOException {
    flush();
  }

  @Override
  public void write(byte[] b) throws IOException {
    outBuff.write(b);
    flush();
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    outBuff.write(b, off, len);
    flush();
  }

  public void setWriteListener(@SuppressWarnings("unused") WriteListener writeListener) {
  }

  public boolean isReady() {
    return true;
  }
}
