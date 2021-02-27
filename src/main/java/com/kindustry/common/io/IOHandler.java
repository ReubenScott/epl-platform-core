package com.kindustry.common.io;import info.monitorenter.cpdetector.io.ASCIIDetector;import info.monitorenter.cpdetector.io.ByteOrderMarkDetector;import info.monitorenter.cpdetector.io.CodepageDetectorProxy;import info.monitorenter.cpdetector.io.JChardetFacade;import info.monitorenter.cpdetector.io.ParsingDetector;import info.monitorenter.cpdetector.io.UnicodeDetector;import java.io.BufferedInputStream;import java.io.BufferedReader;import java.io.ByteArrayInputStream;import java.io.ByteArrayOutputStream;import java.io.File;import java.io.FileInputStream;import java.io.FileNotFoundException;import java.io.FileReader;import java.io.IOException;import java.io.InputStream;import java.io.InputStreamReader;import java.io.RandomAccessFile;import java.io.Reader;import java.net.HttpURLConnection;import java.net.URL;import java.nio.MappedByteBuffer;import java.nio.channels.FileChannel;import java.nio.charset.Charset;import java.util.ArrayList;import java.util.List;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import com.kindustry.system.constant.CharSetType;public class IOHandler {  private static final Logger logger = LoggerFactory.getLogger(IOHandler.class);  /**   * 判断文件的编码格式   *    * @param fileName   *          :file   * @return 文件编码格式   * @throws Exception   */  public static String getCharSetEncoding(String filePath) {    URL url = null;    try {      url = new File(filePath).toURI().toURL();    } catch (IOException e) {      e.printStackTrace();    }    return getCharSetEncoding(url);  }  /**   * 利用第三方开源包cpdetector获取文件编码格式   *    * @param path   *          要判断文件编码格式的源文件的路径   * @author huanglei   * @version 2012-7-12 14:05   */  public synchronized static String getCharSetEncoding(URL url) {    /*     * detector是探测器，它把探测任务交给具体的探测实现类的实例完成。 cpDetector内置了一些常用的探测实现类，这些探测实现类的实例可以通过add方法 加进来，如ParsingDetector、 JChardetFacade、ASCIIDetector、UnicodeDetector。     * detector按照“谁最先返回非空的探测结果，就以该结果为准”的原则返回探测到的 字符集编码。使用需要用到三个第三方JAR包：antlr.jar、chardet.jar和cpdetector.jar cpDetector是基于统计学原理的，不保证完全正确。     */    CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();    /*     * ParsingDetector可用于检查HTML、XML等文件或字符流的编码,构造方法中的参数用于 指示是否显示探测过程的详细信息，为false不显示。     */    detector.add(new ParsingDetector(false));    /*     * JChardetFacade封装了由Mozilla组织提供的JChardet，它可以完成大多数文件的编码 测定。所以，一般有了这个探测器就可满足大多数项目的要求，如果你还不放心，可以 再多加几个探测器，比如下面的ASCIIDetector、UnicodeDetector等。     */    detector.add(JChardetFacade.getInstance());// 用到antlr.jar、chardet.jar    detector.add(ASCIIDetector.getInstance()); // ASCIIDetector用于ASCII编码测定    detector.add(UnicodeDetector.getInstance()); // UnicodeDetector用于Unicode家族编码的测定    detector.add(new ByteOrderMarkDetector());    Charset charset = null;    try {      charset = detector.detectCodepage(url);    } catch (IOException e) {      e.printStackTrace();    }    return CharSetType.getCharSetEncoding(charset.name());  }  /**   * 获取  URL 返回 的   字节   * @param url   * @return   */  public static byte[] getURLByteArray(String url) {    byte[] bin = null;    URL urlfile = null;    HttpURLConnection urlConnection = null;    BufferedInputStream bis = null;    ByteArrayOutputStream out = new ByteArrayOutputStream();    try {      urlfile = new URL(url);      urlConnection = (HttpURLConnection) urlfile.openConnection();      urlConnection.setConnectTimeout(30 * 1000); // 设置连接主机超时（单位：毫秒）      urlConnection.setReadTimeout(30 * 1000); // 设置从主机开始读取数据超时（单位：毫秒）      urlConnection.connect();      bis = new BufferedInputStream(urlConnection.getInputStream());      byte[] buffer = new byte[1024];      int read = 0;      while ((read = bis.read(buffer)) != -1) {        out.write(buffer, 0, read);      }      out.flush();      bin = out.toByteArray();    } catch (Exception e) {      e.printStackTrace();    } finally {      try {        out.close();        bis.close();        if (urlConnection != null) {          urlConnection.disconnect();        }      } catch (IOException e) {        e.printStackTrace();      }    }    return bin;  }  /**   * 获取 URL 返回IO流   * @param url   * @return   */  public static InputStream getURLInputStream(String url) {    return new ByteArrayInputStream(IOHandler.getURLByteArray(url)) ;   }  /**   * 获取字节流 以字节为单位读取文件，常用于读二进制文件，如图片、声音、影像等文件。   *    * @param stream   * @return   * @throws IOException   */  public static byte[] readByteArray(InputStream stream) {    byte[] bt = null;    ByteArrayOutputStream baos = new ByteArrayOutputStream();    try {      byte[] buffer = new byte[4096];      int read = 0;      while ((read = stream.read(buffer)) != -1) {        baos.write(buffer, 0, read);      }      baos.flush();      bt = baos.toByteArray();    } catch (IOException e) {      e.printStackTrace();    } finally {      if (stream != null) {        try {          stream.close();        } catch (IOException e) {          e.printStackTrace();        }      }      if (baos != null) {        try {          baos.close();        } catch (IOException e) {          e.printStackTrace();        }      }    }    return bt;  }  /** 把字节分割 */  public static List<byte[]> splitBytes(byte[] data, byte split) {    List<byte[]> list = new ArrayList<byte[]>();    int index = 0;    List<Byte> temp = new ArrayList<Byte>();    byte[] tb;    while (index < data.length) {      if (data[index] == split) {        tb = new byte[temp.size()];        int i = 0;        for (byte t : temp) {          tb[i++] = t;        }        list.add(tb);      } else {        temp.add(data[index]);      }      index++;    }    return list;  }  /***   * 字节替换   *    * @param data   * @param target   * @param replace   * @return   */  public static byte[] replace(byte[] data, byte target, byte replace) {    for (int i = 0, j = data.length; i < j; i++) {      if (data[i] == target) {        data[i] = replace;      }    }    return data;  }  /**   * 以字节为单位读取文件，常用于读二进制文件，如图片、声音、影像等文件。   */  public static void readFileByBytes(String fileName) {    File file = new File(fileName);    InputStream in = null;    try {      System.out.println("以字节为单位读取文件内容，一次读一个字节：");      // 一次读一个字节      in = new FileInputStream(file);      int tempbyte;      while ((tempbyte = in.read()) != -1) {        System.out.write(tempbyte);      }      in.close();    } catch (IOException e) {      e.printStackTrace();      return;    }    try {      System.out.println("以字节为单位读取文件内容，一次读多个字节：");      // 一次读多个字节      byte[] tempbytes = new byte[100];      int byteread = 0;      in = new FileInputStream(fileName);      System.out.println("当前字节输入流中的字节数为:" + in.available());      // 读入多个字节到字节数组中，byteread为一次读入的字节数      while ((byteread = in.read(tempbytes)) != -1) {        System.out.write(tempbytes, 0, byteread);      }    } catch (IOException e1) {      e1.printStackTrace();    } finally {      if (in != null) {        try {          in.close();        } catch (IOException e1) {        }      }    }  }  /**   * 以字符为单位读取文件，常用于读文本，数字等类型的文件   */  public static void readFileByChars(String fileName) {    File file = new File(fileName);    Reader reader = null;    try {      System.out.println("以字符为单位读取文件内容，一次读一个字节：");      // 一次读一个字符      reader = new InputStreamReader(new FileInputStream(file));      int tempchar;      while ((tempchar = reader.read()) != -1) {        // 对于windows下，\r\n这两个字符在一起时，表示一个换行。        // 但如果这两个字符分开显示时，会换两次行。        // 因此，屏蔽掉\r，或者屏蔽\n。否则，将会多出很多空行。        if (((char) tempchar) != '\r') {          System.out.print((char) tempchar);        }      }      reader.close();    } catch (Exception e) {      e.printStackTrace();    }    try {      System.out.println("以字符为单位读取文件内容，一次读多个字节：");      // 一次读多个字符      char[] tempchars = new char[30];      int charread = 0;      reader = new InputStreamReader(new FileInputStream(fileName));      // 读入多个字符到字符数组中，charread为一次读取字符数      while ((charread = reader.read(tempchars)) != -1) {        // 同样屏蔽掉\r不显示        if ((charread == tempchars.length) && (tempchars[tempchars.length - 1] != '\r')) {          System.out.print(tempchars);        } else {          for (int i = 0; i < charread; i++) {            if (tempchars[i] == '\r') {              continue;            } else {              System.out.print(tempchars[i]);            }          }        }      }    } catch (Exception e1) {      e1.printStackTrace();    } finally {      if (reader != null) {        try {          reader.close();        } catch (IOException e1) {        }      }    }  }  /**   * 以行为单位读取文件，常用于读面向行的格式化文件   */  public static String readFileByLines(String fileName) {    File file = new File(fileName);    BufferedReader reader = null;    StringBuffer content = new StringBuffer();    try {      reader = new BufferedReader(new FileReader(file));      String tempString = null;      int line = 1;      // 一次读入一行，直到读入null为文件结束      while ((tempString = reader.readLine()) != null) {        content.append(tempString);        // 显示行号        line++;      }      reader.close();    } catch (IOException e) {      e.printStackTrace();    } finally {      if (reader != null) {        try {          reader.close();        } catch (IOException e1) {        }      }    }    return content.toString();  }  /**   * 随机读取文件内容   */  public static void readFileByRandomAccess(String fileName) {    RandomAccessFile randomFile = null;    try {      System.out.println("随机读取一段文件内容：");      // 打开一个随机访问文件流，按只读方式      randomFile = new RandomAccessFile(fileName, "r");      // 文件长度，字节数      long fileLength = randomFile.length();      // 读文件的起始位置      int beginIndex = (fileLength > 4) ? 4 : 0;      // 将读文件的开始位置移到beginIndex位置。      randomFile.seek(beginIndex);      byte[] bytes = new byte[10];      int byteread = 0;      // 一次读10个字节，如果文件内容不足10个字节，则读剩下的字节。      // 将一次读取的字节数赋给byteread      while ((byteread = randomFile.read(bytes)) != -1) {        System.out.write(bytes, 0, byteread);      }    } catch (IOException e) {      e.printStackTrace();    } finally {      if (randomFile != null) {        try {          randomFile.close();        } catch (IOException e1) {        }      }    }  }  /**   * 显示输入流中还剩的字节数   */  private static void showAvailableBytes(InputStream in) {    try {      System.out.println("当前字节输入流中的字节数为:" + in.available());    } catch (IOException e) {      e.printStackTrace();    }  }  public static void main(String[] args) {    String fileName = "D:/home/20160318/SS_DICT.del";    // FileReader.readFileByBytes(fileName);    // FileReader.readFileByChars(fileName);    // FileReader.readFileByLines(fileName);    final int BUFFER_SIZE = 0x30;// 缓冲区大小为3M    /**     *      * map(FileChannel.MapMode mode,long position, long size)     *      * mode - 根据是按只读、读取/写入或专用（写入时拷贝）来映射文件，分别为 FileChannel.MapMode 类中所定义的 READ_ONLY、READ_WRITE 或 PRIVATE 之一     *      * position - 文件中的位置，映射区域从此位置开始；必须为非负数     *      * size - 要映射的区域大小；必须为非负数且不大于 Integer.MAX_VALUE     *      * 所以若想读取文件后半部分内容，如例子所写；若想读取文本后1/8内容，需要这样写map(FileChannel.MapMode.READ_ONLY, f.length()*7/8,f.length()/8)     *      * 想读取文件所有内容，需要这样写map(FileChannel.MapMode.READ_ONLY, 0,f.length())     *      */    File file = new File(fileName);    FileChannel fc;    try {      fc = new RandomAccessFile(file, "r").getChannel();      MappedByteBuffer inputBuffer = fc.map(FileChannel.MapMode.READ_ONLY, file.length() / 2, file.length() / 2);      byte[] dst = new byte[BUFFER_SIZE];// 每次读出3M的内容      long start = System.currentTimeMillis();      for (int offset = 0; offset < inputBuffer.capacity(); offset += BUFFER_SIZE) {        if (inputBuffer.capacity() - offset >= BUFFER_SIZE) {          for (int i = 0; i < BUFFER_SIZE; i++)            dst[i] = inputBuffer.get(offset + i);        } else {          for (int i = 0; i < inputBuffer.capacity() - offset; i++)            dst[i] = inputBuffer.get(offset + i);        }        int length = (inputBuffer.capacity() % BUFFER_SIZE == 0) ? BUFFER_SIZE : inputBuffer.capacity() % BUFFER_SIZE;        System.out.println(new String(dst, 0, length));// new        // String(dst,0,length)这样可以取出缓存保存的字符串，可以对其进行操作      }      long end = System.currentTimeMillis();      System.out.println("读取文件文件一半内容花费：" + (end - start) + "毫秒");    } catch (FileNotFoundException e) {      // TODO Auto-generated catch block      e.printStackTrace();    } catch (IOException e) {      e.printStackTrace();    }  }}