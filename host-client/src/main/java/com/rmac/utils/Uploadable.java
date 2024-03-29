package com.rmac.utils;

import com.rmac.RMAC;
import com.rmac.core.MegaClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Signifies a file that is queued to be uploaded by <code>FileUploader</code>
 * <br><br>
 * <cite>
 * Each upload runs as a separate process.
 * </cite>
 *
 * @see com.rmac.core.FileUploader
 */
@Getter
@Setter
@Slf4j
public class Uploadable implements Comparable<Uploadable> {

  private Thread thread;
  private ArchiveFileType type;
  private String file;
  private Consumer<Uploadable> callback;

  public Uploadable(String file, ArchiveFileType type, Consumer<Uploadable> callback) {
    this.type = type;
    this.file = file;
    this.callback = callback;

    this.createThread();
  }

  /**
   * Create a thread to upload the file associated to this uploadable concurrently, but don't start
   * yet, FileUploader process will start the thread based on the file type's priority.
   */
  private void createThread() {
    this.thread = new Thread(() -> {
      try {
        boolean res = RMAC.mega.uploadFile(this.file,
            Utils.getDate() + "/" + Paths.get(this.file).getFileName());

        if (!res) {
          log.error("Unable to upload file, archiving");
          RMAC.archiver.moveToArchive(this.file, this.type);
        } else {
          RMAC.fs.delete(this.file);
        }
      } catch (IOException e) {
        log.error("Could not delete file post-upload", e);
      } finally {
        callback.accept(this);
      }
    });
  }

  /**
   * Start the upload for this file.
   */
  public void execute() {
    this.thread.start();
  }

  /**
   * Compare priorities of this file and the given file.
   *
   * @param o The other uploadable file.
   * @return result (-1=low-prio | 0=same-prio | +1=high-prio)
   */
  @Override
  public int compareTo(Uploadable o) {
    return this.type.getPriority() > o.type.getPriority() ? 1 : -1;
  }
}
