package mp3.son.han;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

/**
 * MP3 Downloader from MP3.Zing.vn and Nhaccuatui.com
 * 
 * How? 
 * - http://mp3.zing.vn 
 * Find the song_ID in the URL Download with
 * http://mp3.zing.vn/download/vip/song/[song_ID] (GZIP encoded) 
 * Since 2015/03: upgraded to http://v3.mp3.zing.vn/download/vip/song/[song_ID] (GZIP encoded)
 * 
 * - http://nhaccuatui.com View source of the song page then find an XML key like
 * http://www.nhaccuatui.com/flash/xml?key1=5f4e297f4c04acbc07c960fca9a2c98c
 * Open this XML URL and view source to find the real URL of the song between
 * tag <location> MP3 file in this way is in 128 bit rate
 * 
 * @author Son Han
 * @version 2015/03 
 * 
 * @todo: 
 * - After timeout msg, it freezes for a while to finalize
 * - Progress bar to adjust interval
 * - Error with album year in the following album (htm instead of 19xx)
 * http://mp3.zing.vn/album/Hybrid-Theory-EP-Linkin-Park/ZWZB9ZAE.html
 */
@SuppressWarnings("serial")
public class ZingMP3Downloader extends JFrame implements ActionListener {

    // Song: http://mp3.zing.vn/bai-hat/Over-Blake-Shelton/ZWZCBEOE.html
    // Album:
    // http://mp3.zing.vn/album/Dieu-Chua-Tung-Noi-Bich-Phuong/ZWZBAWI0.html
    
    /* Random interval between songs to download = BASE > BASE + INTERVAL - 1 */
    public static final int RANDOM_INTERVAL = 8;
    public static final int RANDOM_BASE = 3;
    
    private final int BORDER_TOP = 5;
    private final int BORDER_LEFT = 5;
    private final int BORDER_BOTTOM = 5;
    private final int BORDER_RIGHT = 0;

    private final int INSETS_TOP = 2;
    private final int INSETS_LEFT = 2;
    private final int INSETS_BOTTOM = 2;
    private final int INSETS_RIGHT = 2;

    private final int URL_FIELD_WIDTH = 300;
    private final int URL_FIELD_HEIGHT = 10;

    private final int TIMEOUT = 30; // minutes

    private final String PATTERN_ZING_ALBUM_SONGID = "<li id=\"song(.*?)\" class=\"fn-playlist";
    private final String PATTERN_ZING_ALBUM_INFO = ";name=(.*?)\" data-ad-status=";
    private final String PATTERN_ZING_ALBUM_YEAR = "itemprop=\"copyrightYear\">(.*?)</div>";
    private final String PATTERN_NCT_SONG = "key1=(.*?)\" /><embed";

    private final String MSG_SONG_DOWNLOAD = "File loaded, check your browser for downloading progress!";
    private final String MSG_INVALID_URL = "Invalid URL!";
    private final String MSG_HELP = "<html>"
            + "*Prepare <br>"
            + "- Put this program in the download folder of your default browser <br>"
            + "- Find the song or album you want to download<br>"
            + "- Copy its URL to the URL box and press Download<br><br>"
            + "*Download<br>"
            + "- Several Web browser windows (or tabs) will be automatically opened and closed<br>"
            + "- Wait until all files downloaded<br><br>"
            + "*Stop<br>"
            + "- Click Stop button if an error prevents the program from finishing<br>"
            + "*If a song fails<br>"
            + "- Search for an alternative URL and re-download!<br><br>"
            + "Enjoy the ultimate music, thanks to Zing MP3!<br>"
            + "Cheers!<br><br>" + "Son</html>";

    private final String nctBase = "http://www.nhaccuatui.com";
    private final String nctSongExt = "/bai-hat";

    private final String zingSongLocationBase = "http://v3.mp3.zing.vn/download/vip/song/";
    private final String zingBase = "http://mp3.zing.vn";
    private final String zingSongExt = "/bai-hat";
    private final String zingAlbumExt = "/album";
    private final String zingPlaylistExt = "/playlist";

    private JTextField urlField = new JTextField();
    private JTextArea infoField = new JTextArea();
    private JProgressBar progressBar = new JProgressBar();
    private JButton downloadButton = new JButton("Download");
    private JButton stopButton = new JButton("Stop");

    private String albumInfo;
    private DownloadingWorker worker;
    private String htmlCode;

    private ImageIcon icon = new ImageIcon(
            ZingMP3Downloader.class.getResource("/res/favicon.png"));

    /*
     * Estimated size of one song in 320kbps, samples from Beth Hart albums
     * (144/14 + 112/11 + 119/13 + 114/12 + 122/13 + 105/11 + 139/13)/7 = 9.82
     * (MB)
     */
    // private final int songSize = 10; // 10 MB

    private String album = "tmp";
    private String artist = "";
    String key, albumTitle = "test";
    String albumYear = "test";

    private List<String> song_ids = new ArrayList<>();

    private Timer timer_downloading;
    private File albumDir;

    private long album_download_starttime;

    public ZingMP3Downloader() {
        super("MP3 Downloader from mp3.zing.vn, nhaccuatui.com");
        this.setIconImage(icon.getImage());

        timer_downloading = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File curDir = new File(".");
                File[] songs = curDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".mp3");
                        // && name.contains(artist);
                    }
                });

                progressBar.setString(songs.length + "/" + song_ids.size()
                        + " downloaded!");

                // Check every 500ms there are how many files downloaded
                // If the number is equal to the album size > stop
                if (songs.length == song_ids.size()) {
                    finalizeDownload(songs);
                }

                // Check timeout 1 min = 60K milisecond
                if (System.currentTimeMillis() - album_download_starttime > TIMEOUT * 60000) {
                    
                    showMessage("Timeout: Only " + songs.length + "/"
                            + song_ids.size() + " files downloaded");
                    
                    finalizeDownload(songs);
                }
            }
        });

        createUI();
    }

    private void createUI() {
        JPanel content = new JPanel();
        JLabel titleLabel = new JLabel("Song/Album URL");
        titleLabel.setToolTipText(MSG_HELP);

        infoField.setEditable(false);
        infoField.setLineWrap(true);
        infoField.setBackground(getBackground());
        infoField.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        // sourceField.setText(MSG_HELP);

        urlField.setPreferredSize(new Dimension(URL_FIELD_WIDTH,
                URL_FIELD_HEIGHT));

        urlField.addActionListener(this);
        downloadButton.addActionListener(this);
        stopButton.addActionListener(this);

        stopButton.setVisible(false);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        JLabel helpLabel = new JLabel("");
        helpLabel.setToolTipText(MSG_HELP);

        content.setBorder(BorderFactory.createEmptyBorder(BORDER_TOP,
                BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT));
        content.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM,
                INSETS_RIGHT);

        c.gridy = 0;
        c.weightx = 0;
        content.add(titleLabel, c);

        c.weightx = 1;
        content.add(urlField, c);

        c.weightx = 0;
        content.add(downloadButton, c);

        c.gridy++;
        content.add(helpLabel, c);

        c.gridx = 1;
        content.add(progressBar, c);

        c.gridheight = 1;
        c.gridx = 2;
        content.add(stopButton, c);

        c.gridy++;
        c.gridx = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 3;
        content.add(infoField, c);

        this.setContentPane(content);
        // this.pack();
        this.setSize(500, 150);
        this.setVisible(true);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String url = urlField.getText();
        Pattern PATTERN;
        Matcher m;

        if (e.getSource() == stopButton) {
            stopDownloading();
        } else if (url.contains(zingBase + zingSongExt)) {
            int i = url.lastIndexOf("/");
            int j = url.lastIndexOf(".html");
            if (i > 0 && i < j) {
                try {
                    // downloadSong(url.substring(i + 1, j));
                    downloadFileViaBrowser(url.substring(i + 1, j));
                    Thread.sleep(1000);
                    showMessage(MSG_SONG_DOWNLOAD);
                    // reset();
                } catch (Exception ex) {
                    showMessage(ex.toString());
                }
            } else {
                showMessage(MSG_INVALID_URL);
            }
        } else if (url.contains(zingBase + zingAlbumExt)
                || url.contains(zingBase + zingPlaylistExt)) {
            if (checkForExistingMP3File()) {
                showMessage("Please remove all MP3 files in the current directory!");
            } else {
                urlField.setEnabled(false);
                downloadButton.setEnabled(false);
                stopButton.setVisible(true);
                progressBar.setVisible(true);
                progressBar.setIndeterminate(true);
                progressBar.setString("Retrieving data...");

                album_download_starttime = System.currentTimeMillis();
                worker = new DownloadingWorker(url);
                worker.execute();
            }

        } else if (url.contains(nctBase + nctSongExt)) {
            try {
                String text = getHTML(url, false);
                // file=http://www.nhaccuatui.com/flash/xml?key1=5f4e297f4c04acbc07c960fca9a2c98c"
                // /><embed

                PATTERN = Pattern.compile(PATTERN_NCT_SONG);
                m = PATTERN.matcher(text);
                if (m.find()) {
                    String key = m.group(1);
                    text = getHTML("http://www.nhaccuatui.com/flash/xml?key1="
                            + key, false);
                    // System.out.println(text);

                    int beginIndex = text.lastIndexOf("<location>");
                    int endIndex = text.lastIndexOf("</location>");

                    downloadFileViaBrowser(text.substring(beginIndex + 28,
                            endIndex - 8));

                    Thread.sleep(1000);
                    showMessage(MSG_SONG_DOWNLOAD);
                    // System.out.println(text.substring(beginIndex + 28,
                    // endIndex - 8));
                }

            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        } else {
            showMessage(MSG_INVALID_URL);
        }
    }

    private boolean checkForExistingMP3File() {
        File curDir = new File(".");
        File[] songs = curDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mp3");
                // && name.contains(artist);
            }
        });
        return (songs.length > 0);
    }

    private void stopDownloading() {
        // System.out.println("prepare to stop..." + album + artist);

        File curDir = new File(".");
        File[] songs = curDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mp3");
                // && name.contains(artist);
            }
        });

        finalizeDownload(songs);

    }

    /**
     * @param files
     * @return move all downloaded files to the Album folder and reset the UI
     */
    private int finalizeDownload(File[] files) {
        timer_downloading.stop();
        if (!worker.isCancelled()) {
            worker.cancel(true);
        }
        progressBar.setVisible(false);
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);

        urlField.setEnabled(true);
        urlField.setText("");
        downloadButton.setEnabled(true);
        stopButton.setVisible(false);

        albumDir = createAlbumDirectory();
        if (albumDir == null) {
            showMessage("Folder error: [" + album + "]/[" + artist + "]");
            return 0;
        }

        int moved = 0;

        for (File file : files) {
            try {
                zingUpdate(file, artist, albumTitle, albumYear);
            } catch (UnsupportedTagException | InvalidDataException
                    | NotSupportedException | IOException e) {
                e.printStackTrace();
            }

            if (file.renameTo(new File(albumDir, file.getName())))
                moved++;
        }

        Toolkit.getDefaultToolkit().beep();
        reset();
        return moved;
    }

    private void reset() {
        song_ids.clear();
        album = ".";
        artist = "";
    }

    private File createAlbumDirectory() {
        // Create Artist directory
        File file = new File(artist);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created: " + file.toString());
            } else {
                System.out.println("Failed to create directory: "
                        + file.toString());
                return null;
            }
        }

        // Fix album name
        album = album.replaceAll("[/?<>\\:*|]", "");

        // Under Artist directory, create album directory: Album_Name
        file = new File(artist + "/" + album);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created: " + file.toString());
                return file;
            } else {
                System.out.println("Failed to create directory: "
                        + file.toString());
                return null;
            }
        } else {
            System.out.println("File existed: " + file.toString());
            return file;
        }
    }

    public static boolean isValidName(String text) {
        Pattern pattern = Pattern.compile("[^/./\\:*?\"<>|]");
        return !pattern.matcher(text).find();
    }

    private void downloadFileViaBrowser(String URL) throws IOException,
            URISyntaxException {
        Desktop.getDesktop().browse(new URI(zingSongLocationBase + URL));
    }

    private void downloadFileSilence(String songID) throws IOException,
            URISyntaxException {
        String urlString = zingSongLocationBase + songID;

        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        System.out.println("orignal url: " + conn.getURL());
        conn.connect();
        System.out.println("connected url: " + conn.getURL());

        // open gzip (compressed) stream to download --> error
        // GZIPInputStream in = new GZIPInputStream(conn.getInputStream());

        // open normal stream to download --> error
        BufferedInputStream in = new BufferedInputStream(url.openStream());

        FileOutputStream fout = new FileOutputStream("testdownload.mp3");

        final byte data[] = new byte[1024];
        int count;
        while ((count = in.read(data, 0, 1024)) != -1) {
            fout.write(data, 0, count);
        }
        if (in != null)
            in.close();

        if (fout != null)
            fout.close(); //

    }

    private String getHTML(String address, boolean isGZIP) throws Exception {
        URL url = new URL(address);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        InputStream is;
        if (isGZIP) {
            is = new GZIPInputStream(conn.getInputStream());
        } else {
            is = conn.getInputStream();
        }
        // open the stream and put it into BufferedReader
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        // Charset.forName("UTF-8")
        // conn.getContentEncoding()
        String output = "";
        String inputLine;
        while ((inputLine = br.readLine()) != null) {
            // System.out.println(inputLine);
            output += inputLine + "\n";
        }
        br.close();
        return output;
    }

    private void showMessage(String msg) {
        System.out.println(msg);
        JOptionPane.showMessageDialog(this, msg, "Message",
                JOptionPane.INFORMATION_MESSAGE, icon);
    }

    /**
     * For time-consuming task to update Swing: downloading album
     * 
     */
    class DownloadingWorker extends SwingWorker<String, Void> {
        // private String htmlCode;
        private String url;

        // private long before;

        DownloadingWorker(String url) {
            this.url = url;
        }

        @Override
        protected String doInBackground() throws IOException {
            try {
                // before = System.currentTimeMillis();
                htmlCode = getHTML(url, true);

                // long speed = 15 * htmlCode.length()
                // / (System.currentTimeMillis() - before); // ~ kb per
                // second
                int i = 0;

                // while ((i = htmlCode.indexOf("add-lyric", i) + 1) > 0) {
                // song_ids.add(htmlCode.substring(i - 18, i - 10));
                // }

                Pattern PATTERN = Pattern.compile(PATTERN_ZING_ALBUM_SONGID);
                Matcher m = PATTERN.matcher(htmlCode);
                while (m.find()) {
                    song_ids.add(m.group(1));
                }

                System.out.println(song_ids);

                if (song_ids.size() == 0) {
                    showMessage("No song found, check the URL again!");
                    stopDownloading();
                    return "";
                }

                PATTERN = Pattern.compile(PATTERN_ZING_ALBUM_INFO);
                m = PATTERN.matcher(htmlCode);
                if (m.find()) {
                    String info = m.group(1);
                    String[] infos = info.split(";singer=");
                    albumTitle = infos[0];
                    albumTitle = albumTitle.replace('-', ' ');
                    artist = infos[1];
                    artist = artist.replace('-', ' ');
                }

                key = "h\u00E0nh:</span> ";
                i = htmlCode.indexOf(key);
                albumYear = htmlCode.substring(i + key.length(),
                        i + key.length() + 4);

                PATTERN = Pattern.compile(PATTERN_ZING_ALBUM_YEAR);
                m = PATTERN.matcher(htmlCode);
                if (m.find()) {
                    albumYear = m.group(1);
                }

                album = albumTitle + " (" + albumYear + ")";
                album = album.replace("&#039;", "'");
                // album = album.replace('-', ' ');

                albumInfo = artist + "\n" + album + "\n" + song_ids.size()
                        + " tracks";

                infoField.setText(albumInfo);

                progressBar.setString("Downloading!");
                timer_downloading.start();

                System.out.println(album + artist);
                /*
                 * Randomly launch the browser to download each file to avoid
                 * DOS-like request
                 */
                Random ran = new Random();
                for (String songID : song_ids) {
                    int pause = (ran.nextInt(RANDOM_INTERVAL) + RANDOM_BASE) * 1000;
                    try {
                        downloadFileViaBrowser(songID);
                        Thread.sleep(pause);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return htmlCode;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";

        }

        @Override
        protected void done() {
        }
    }

    private void zingUpdate(File file, String artist,
            String album, String year) throws UnsupportedTagException, InvalidDataException, IOException, NotSupportedException {
        Mp3File mp3file = new Mp3File(file);
        ID3v2 id3v2Tag;
        if (mp3file.hasId3v2Tag()) {
          id3v2Tag = mp3file.getId3v2Tag();
        } else {
          // mp3 does not have an ID3v2 tag, let's create one..
          id3v2Tag = new ID3v24Tag();
          mp3file.setId3v2Tag(id3v2Tag);
        }
        id3v2Tag.setArtist(artist);
        id3v2Tag.setAlbum(album);
        id3v2Tag.setAlbumArtist(artist);
        id3v2Tag.setYear(year);
        
        id3v2Tag.setComment("");
        id3v2Tag.setPublisher("Chateau d'Alfortville");
        id3v2Tag.setCopyright("Son Han");
        
        mp3file.saveOverride(file);
    }
    
    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ZingMP3Downloader();
            }
        });
    }
}
