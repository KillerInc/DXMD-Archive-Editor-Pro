import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
final class CompressedResource { private CompressedResource(){} static BufferedReader open(Class<?> owner,String resource)throws IOException{InputStream in=owner.getResourceAsStream(resource);if(in==null)throw new FileNotFoundException(resource);ByteArrayOutputStream encoded=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>=0)encoded.write(buf,0,n);byte[] gz=Base64.getMimeDecoder().decode(encoded.toByteArray());return new BufferedReader(new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(gz)),StandardCharsets.UTF_8));}}
