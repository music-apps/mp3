package mp3.son.han;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

public class MP3TagToolkit {
    public static void main(String[] args) throws IOException, Exception {
        // test
    }

    public static void updateTag(File file, String tagName, String tagVal) throws UnsupportedTagException, InvalidDataException, IOException, NotSupportedException {
        Mp3File mp3file = new Mp3File(file);
        ID3v2 id3v2Tag;
        if (mp3file.hasId3v2Tag()) {
          id3v2Tag = mp3file.getId3v2Tag();
        } else {
          // mp3 does not have an ID3v2 tag, let's create one..
          id3v2Tag = new ID3v24Tag();
          mp3file.setId3v2Tag(id3v2Tag);
        }
        
        setTag(id3v2Tag, tagName, tagVal);
        
        mp3file.saveOverride(file);
    }
    
    public static void updateTags(File file, List<String> tagNames, List<String> tagVals) throws UnsupportedTagException, InvalidDataException, IOException, NotSupportedException {
        Mp3File mp3file = new Mp3File(file);
        ID3v2 id3v2Tag;
        if (mp3file.hasId3v2Tag()) {
          id3v2Tag = mp3file.getId3v2Tag();
        } else {
          // mp3 does not have an ID3v2 tag, let's create one..
          id3v2Tag = new ID3v24Tag();
          mp3file.setId3v2Tag(id3v2Tag);
        }
        
        for(int i = 0, n = tagNames.size(); i < n; i++) { 
            String tagName = tagNames.get(i);
            String tagVal = tagVals.get(i);
            setTag(id3v2Tag, tagName, tagVal);
        }
        
        mp3file.saveOverride(file);
    }
    
    private static void setTag(ID3v2 id3v2Tag, String tagName, String tagVal) {
        if (tagName.toLowerCase().equals("artist")) id3v2Tag.setArtist(tagVal);
        if (tagName.toLowerCase().equals("albumartist")) id3v2Tag.setAlbumArtist(tagVal);
        if (tagName.toLowerCase().equals("album")) id3v2Tag.setAlbum(tagVal);
        if (tagName.toLowerCase().equals("comment")) id3v2Tag.setComment(tagVal);
        if (tagName.toLowerCase().equals("copyright")) id3v2Tag.setCopyright(tagVal);
        if (tagName.toLowerCase().equals("composer")) id3v2Tag.setComposer(tagVal);
        if (tagName.toLowerCase().equals("gerne")) id3v2Tag.setGenreDescription(tagVal);
        if (tagName.toLowerCase().equals("title")) id3v2Tag.setTitle(tagVal);
        if (tagName.toLowerCase().equals("track")) id3v2Tag.setTrack(tagVal);
        if (tagName.toLowerCase().equals("year")) id3v2Tag.setYear(tagVal);
        if (tagName.toLowerCase().equals("publisher")) id3v2Tag.setPublisher(tagVal);

    }

    public static void updateDirectory(File dir, List<String> tagNames,
            List<String> tagVals) throws Exception {
        
        if (!dir.isDirectory()) throw new Exception("Not directory");
        
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mp3");
                // && name.contains(artist);
            }
        });
        
        if (files != null) for (File file : files) {
            MP3TagToolkit.updateTags(file, tagNames, tagVals);
        }
    }
}
