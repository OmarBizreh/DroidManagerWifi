import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FileSender extends AsyncTask<String, Integer, String> {

	public FileSender() {
		this.ConstructorFlag = 0;
	}

	public FileSender(TextView txtProgress, String Message) {
		this.ConstructorFlag = 1;
		this.ProgressMessage = Message;
		this.txtProgress = txtProgress;
	}

	public FileSender(TextView txtProgress, ProgressBar barProgress,
			String Message) {
		this.ConstructorFlag = 2;
		this.txtProgress = txtProgress;
		this.barProgress = barProgress;
		this.ProgressMessage = Message;
	}

	private int ConstructorFlag;
	private Socket socket;
	private String ProgressMessage;
	private TextView txtProgress;
	private ProgressBar barProgress;

	private String SourceIP = "", SourceFileName = "";
	private int intFileSize = 0, bytesSent = 0;
	private PrintWriter stringOutputStream = null;

	@Override
	protected String doInBackground(String... params) {
		// TODO Auto-generated method stub

		try {
			/*
			 * Destination IP Address is provided as the first element in the
			 * params array. The rest of the array elements are path of files to
			 * be sent to computer. [Currently folder sending is not supported].
			 */
			SourceIP = params[0];

			socket = new Socket(SourceIP, 8000);
			try {
				for (int i = 1; i < params.length; i++) {
					SourceFileName = params[i];
					File fileToSend = new File(SourceFileName);
					FileInputStream fi = new FileInputStream(fileToSend);
					intFileSize = Integer.valueOf(String.valueOf(fileToSend
							.length()));

					/*
					 * Send File name and size to Droid Manager. It must be sent
					 * that way because Droid Manager treats first received
					 * package as file information holder. NOTE: File size is
					 * important because Droid Manager goes through a loop while
					 * receiving byte[], file size gets compared with number of
					 * received bytes to determine when to break the loop.
					 * Reading the while loop below will give a clear idea about
					 * what I just meant.
					 */
					stringOutputStream = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					stringOutputStream.println(SourceFileName + ";"
							+ intFileSize + ";");
					stringOutputStream.flush();

					/* Now time to send the actual file. */
					OutputStream dataOutputStream = socket.getOutputStream();
					int bytesRead = 0;
					byte[] test = new byte[4194304]; // Array size can be
														// changed although
														// I
														// chose to make it
														// 4MB
														// after doing a
														// research and
														// found
														// it's safe for
														// Android
														// to send and
														// receive
														// 4MB of data in a
														// single
														// dataOutputStream.write();
														// (or read).
					// ---------------------------------------
					// Reading bytes of file and filling it in the "test"
					// byte array.
					while (-1 != (bytesRead = fi.read(test, 0, test.length))) {
						bytesSent += bytesRead;
						// Sending our data through the socket.
						dataOutputStream.write(test, 0, bytesRead);
						// Comparing amount of bytes sent with the size of
						// the
						// file, although it is checked during the while
						// loop, but when testing the code I came through
						// issues
						// resulting in breaking the file so I had to
						// compare
						// amount of bytes sent with the size of file being
						// sent.
						if (bytesSent == intFileSize) {
							break;
						}
						publishProgress((bytesSent % intFileSize));
					}
					// Flushing the output stream just in-case the last 4MB
					// aren't sent by the device.
					dataOutputStream.flush();
					// Closing what needs to be closed.
					dataOutputStream.close();
					fi.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					socket.close();
					bytesSent = 0;
				} catch (Exception e2) {
					// TODO: handle exception
					e2.printStackTrace();
				}
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		switch (this.ConstructorFlag) {
		case 1:
			txtProgress.setText(this.ProgressMessage + values[0]);
			break;
		case 2:
			txtProgress.setText(this.ProgressMessage + values[0]);
			barProgress.setProgress(values[0]);
			break;
		default:
			break;
		}
	}

}