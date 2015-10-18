package mp3.son.han;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Son Han
 * @todo: use Reflection to map function to tagName
 *
 */
public class mp3tags {

    public static void main(String[] args) throws IOException, Exception {
        List<String> tagNames = new ArrayList<>();
        List<String> tagVals = new ArrayList<>();
        
        if (args.length < 3)    usage();
        
        // first parameter file/directory PATH
        File target = new File(args[0]);
        
        // tags
        for(int i = 1, n = args.length; i < n; i++) {
            if (args[i].startsWith("-")) {
                tagNames.add(args[i].substring(1));
                tagVals.add(args[++i]);
            }
        }
        
        if (target.isFile()) MP3TagToolkit.updateTags(target, tagNames, tagVals);
        
        else if (target.isDirectory()) {
            updateDir(target, tagNames, tagVals);
//            // Update tags of files in the directory
//            MP3TagToolkit.updateDirectory(target, tagNames, tagVals);
//            
//            List<File> subDirs = subDirs(target);    
//            for (File subDir : subDirs) {
//                // Update sub-directory
//                MP3TagToolkit.updateDirectory(subDir, tagNames, tagVals);
//                List<File> subSubDirs = subDirs(subDir);
//                for (File subSubDir : subSubDirs) {
//                    // Update sub-sub-directory
//                    MP3TagToolkit.updateDirectory(subSubDir, tagNames, tagVals);
//                }
//            }
        }
        
        else System.out.print(target.getAbsolutePath() + ": Not file or directory");
    }
    
    private static void updateDir(File dir, List<String> tagNames, List<String> tagVals) throws Exception {
        MP3TagToolkit.updateDirectory(dir, tagNames, tagVals);
        List<File> subDirs = subDirs(dir);
        if (subDirs.size() > 0)
            for (File subDir : subDirs)
                updateDir(subDir, tagNames, tagVals);
    }

    private static List<File> subDirs(File dir) {
        File[] files = dir.listFiles();
        List<File> subDirs = new ArrayList<>();
        
        if (files != null) for (File file : files) {
            if (file.isDirectory()) subDirs.add(file);
        }
        return subDirs;
    }
    
    private static void usage() {
        System.err.println("Usage:  java -jar mp3tags [PATH: file or directory] [-TAGNAME] [TAGVAL]");
        System.err.println("        Update MP3 tags of all MP3 files in PATH including subdirs"); 
        System.err.println("Use \" \" to embrace parameters with white space. Example:");
        System.err.println("       java -jar mp3tags -artist \"Pink Floyd\"");
        System.err.println("Tag names: ");
        System.err.println("       -artist : Contributing artists");
        System.err.println("       -albumartist : Album artist");
        System.err.println("       -album : Album name");
        System.err.println("       -comment : Comment");
        System.err.println("       -copyright : Copy Right");
        System.err.println("       -composer : Composer");
        System.err.println("       -gerne : Gerne");
        System.err.println("       -title : Title");
        System.err.println("       -track : Track");
        System.err.println("       -year : Year");
        System.err.println("       -publisher : Publisher");
             
        System.exit(0);
      }
}
