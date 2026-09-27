import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Maps physical archive offsets back to the internal resource/chunk declared by DXMD's ARCH directory. */
final class ArchiveResourceIndex {
    record Location(String resourceName, int chunkIndex, long resourceOffset, long archiveOffset, long chunkLength) {}
    private record Region(long start, long end, String resourceName, int chunkIndex, long resourceBegin) {}
    private record CacheEntry(long size, long modified, ArchiveResourceIndex index) {}

    private static final HashMap<String,CacheEntry> CACHE = new HashMap<>();

    private final ArrayList<Region> regions = new ArrayList<>();
    private final String archiveName;

    private ArchiveResourceIndex(String archiveName) { this.archiveName = archiveName; }

    static synchronized ArchiveResourceIndex load(File archive) throws IOException {
        if (archive == null || !archive.isFile()) throw new FileNotFoundException("Archive not found.");
        String path=archive.getCanonicalPath();
        long size=archive.length(), modified=archive.lastModified();
        CacheEntry hit=CACHE.get(path);
        if(hit!=null && hit.size==size && hit.modified==modified) return hit.index;

        ArchiveResourceIndex out = parse(archive);
        CACHE.put(path,new CacheEntry(size,modified,out));
        return out;
    }

    private static ArchiveResourceIndex parse(File archive) throws IOException {
        ArchiveResourceIndex out = new ArchiveResourceIndex(archive.getName());
        try (RandomAccessFile raf = new RandomAccessFile(archive, "r")) {
            byte[] magic = new byte[4]; raf.readFully(magic);
            if (!Arrays.equals(magic, new byte[]{'A','R','C','H'})) throw new IOException("Not a DXMD ARCH archive: " + archive.getName());
            readIntLE(raf); // unknown/version
            int fileCount = readIntLE(raf);
            int linkedCount = readIntLE(raf);
            long directoryOffset = readLongLE(raf);
            if (fileCount < 0 || linkedCount < 0 || directoryOffset < 0 || directoryOffset >= raf.length())
                throw new IOException("Invalid ARCH directory metadata.");
            raf.seek(directoryOffset);
            ArrayList<String> links = new ArrayList<>(linkedCount);
            for (int i=0;i<linkedCount;i++) {
                int n=readIntLE(raf); links.add(readString0(raf,n));
            }
            int selfIndex=-1;
            for (int i=0;i<links.size();i++) if (links.get(i).equalsIgnoreCase(archive.getName())) { selfIndex=i; break; }
            if (selfIndex < 0 && linkedCount == 1) selfIndex=0; // some DLC archives use a single self-link with a variant path/name
            for (int fi=0; fi<fileCount; fi++) {
                raf.skipBytes(24);
                int nameLen=readIntLE(raf);
                String name=readString0(raf,nameLen);
                int chunkCount=readIntLE(raf);
                for (int ci=0; ci<chunkCount; ci++) {
                    int archiveIndex=readIntLE(raf);
                    long resourceBegin=readLongLE(raf);
                    long archiveOffset=readLongLE(raf);
                    long length=readLongLE(raf);
                    if (archiveIndex==selfIndex && length>0 && archiveOffset>=0 && archiveOffset+length<=raf.length())
                        out.regions.add(new Region(archiveOffset,archiveOffset+length,name,ci,resourceBegin));
                }
            }
        }
        out.regions.sort(Comparator.comparingLong(Region::start));
        return out;
    }

    Location locate(long archiveOffset) {
        int lo=0, hi=regions.size()-1;
        while (lo<=hi) {
            int mid=(lo+hi)>>>1; Region r=regions.get(mid);
            if (archiveOffset<r.start) hi=mid-1;
            else if (archiveOffset>=r.end) lo=mid+1;
            else return new Location(r.resourceName,r.chunkIndex,r.resourceBegin+(archiveOffset-r.start),archiveOffset,r.end-r.start);
        }
        return null;
    }

    int regionCount(){ return regions.size(); }
    String archiveName(){ return archiveName; }

    private static String readString0(RandomAccessFile raf,int n) throws IOException {
        if (n<0 || n>16*1024*1024) throw new IOException("Invalid ARCH string length: "+n);
        byte[] b=new byte[n]; raf.readFully(b); int zero=raf.read();
        if (zero<0) throw new EOFException();
        return new String(b,StandardCharsets.UTF_8);
    }
    private static int readIntLE(RandomAccessFile raf) throws IOException {
        int a=raf.read(),b=raf.read(),c=raf.read(),d=raf.read(); if((a|b|c|d)<0)throw new EOFException();
        return a|(b<<8)|(c<<16)|(d<<24);
    }
    private static long readLongLE(RandomAccessFile raf) throws IOException {
        long v=0; for(int i=0;i<8;i++){int b=raf.read();if(b<0)throw new EOFException();v|=((long)b)<<(8*i);} return v;
    }
}
