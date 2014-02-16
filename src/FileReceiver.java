import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FileReceiver extends AsyncTask<String, Integer, String> {

	public FileReceiver() {
		this.ConstructorFlag = 0;
	}

	public FileReceiver(TextView txtProgress, String Message) {
		this.ConstructorFlag = 1;
		this.ProgressMessage = Message;
		this.txtProgress = txtProgress;
	}

	public FileReceiver(TextView txtProgress, ProgressBar barProgress,
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

	@Override
	protected String doInBackground(String... params) {
		// TODO Auto-generated method stub
		try {
			/*
			 * Droid Manager listens to port 8001 for Receiving, and 8000 for
			 * sending to allow both processes to go. When using same port
			 * socket exception is raised stating port already in use.
			 */
			socket = new Socket(params[0], 8001);
			/*
			 * To determine if a file or folder is being received, the first
			 * byte received is here to let us know. Since Droid Manager knows
			 * what it's sending it let's the android app know from by sending
			 * "0" if the upcoming bytes are for file information & "1" if the
			 * upcoming bytes are folder information
			 */
			InputStreamReader inStream = new InputStreamReader(
					socket.getInputStream());
			/*
			 * After doing tests when this code was initially written, I
			 * encountered issues leading to receiving more than one byte
			 * although Droid Manager send 1 byte only, it was solved by reading
			 * into a temp char[] with a fixed size then create a new array with
			 * the number of bytes read. NOTE: Why char[] flag size = 2? Because
			 * 0 or 1 in ASCII are represented in two numbers (i.e. letter a =
			 * 97 in ascii)
			 */
			char[] flag = new char[2];

			int bytes = inStream.read(flag);

			char[] FinalFlag = new char[bytes];

			for (int i = 0; i < bytes; i++) {
				FinalFlag[i] = flag[i];
			}
			if (String.valueOf(FinalFlag).equals("0")) {
				// DestinationAddress is chosen by the user.
				ReceiveFile(params[1]);
			} else {
				// Same here DestinationAddress is chosen by the user.
				ReceiveFolders(params[1]);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}

	public void ReceiveFolders(String DestinationAddress) {
		try {
			/*
			 * Read folder information which contains the following: 1. Folder
			 * Name. 2. First File Name. 3. First File Size. Droid Manager
			 * handles sending all files inside the folder, that's why number of
			 * files inside the folder is sent to the device
			 */
			InputStreamReader inStream = new InputStreamReader(
					socket.getInputStream());

			String strFileName = "";
			// First: information received is saved into a random char
			// array.
			char[] Folder = new char[1024];
			int intFolder = inStream.read(Folder);
			/*
			 * 1024 for a file name!? Since file name is unknown so we must
			 * cover all possibilites that's why another for loop is executed so
			 * that we take only the items of "Folder" array that actually holds
			 * the file name.
			 */
			char[] strFolder = new char[intFolder];
			for (int i = 0; i < intFolder; i++) {
				strFolder[i] = Folder[i];
			}
			// Time to handle creating the folder on the device.
			String path;
			if (DestinationAddress.length() == 0) {
				path = android.os.Environment.getExternalStorageDirectory()
						.getPath() + "/" + String.valueOf(strFolder);
			} else {
				path = DestinationAddress + String.valueOf(strFolder);
			}
			File fold = new File(path);
			if (!fold.exists()) {
				fold.mkdirs();
			}
			// And now time to start receiving the file.
			char[] FileName = new char[1024]; // Just like before, we don't
												// know file name length so
												// a temp array is used
			int bytesRead = inStream.read(FileName);
			char[] FinalFileName = new char[bytesRead];
			for (int i = 0; i < bytesRead; i++) {
				FinalFileName[i] = FileName[i];
			}

			char[] FileSize = new char[4194304]; // this array can be
													// anysize although I
													// chose 4MB to be safe
													// according to research
													// results.
			int BytesRead2 = inStream.read(FileSize);
			char[] FinalFileSize = new char[BytesRead2];
			for (int i = 0; i < BytesRead2; i++) {
				FinalFileSize[i] = FileSize[i];
			}
			strFileName = new String(FinalFileName);

			String strFileSize = new String(FinalFileSize);

			int intFileSize = Integer.valueOf(strFileSize);
			byte[] buffer = new byte[4194304]; // The actual array that will
												// include bytes of the file
												// received
			int b = 0;

			int receivedBytes = 0;

			strFileName = "/" + strFileName;

			path += strFileName;

			File ReceivedFile = new File(path);

			ReceivedFile.createNewFile();

			FileOutputStream fout = new FileOutputStream(ReceivedFile);

			InputStream ins = socket.getInputStream();

			while ((b = ins.read(buffer)) > 0) {
				fout.write(buffer, 0, b);
				fout.flush();
				receivedBytes += b;

				publishProgress((receivedBytes % intFileSize));
				
				if (receivedBytes == intFileSize || isCancelled()) {
					break;
				}
			}

			socket.close();
			fout.close();
		} catch (final Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public void ReceiveFile(String DestinationAddress) {
		try {
			InputStreamReader inStream = new InputStreamReader(
					socket.getInputStream());
			String strFileName = "";
			char[] FileName = new char[1024];
			int bytesRead = inStream.read(FileName);
			char[] FinalFileName = new char[bytesRead];
			for (int i = 0; i < bytesRead; i++) {
				FinalFileName[i] = FileName[i];
			}
			char[] FileSize = new char[1024];
			int BytesRead2 = inStream.read(FileSize);
			char[] FinalFileSize = new char[BytesRead2];
			for (int i = 0; i < BytesRead2; i++) {
				FinalFileSize[i] = FileSize[i];
			}
			strFileName = new String(FinalFileName);

			String strFileSize = new String(FinalFileSize);

			int intFileSize = Integer.valueOf(strFileSize);
			byte[] buffer = new byte[4194304];
			int b = 0;
			int receivedBytes = 0;
			strFileName = "/" + strFileName;
			String path = DestinationAddress + strFileName;
			File ReceivedFile = new File(path);
			ReceivedFile.createNewFile();
			FileOutputStream fout = new FileOutputStream(ReceivedFile);

			barProgress.setMax(intFileSize);

			InputStream ins = socket.getInputStream();
			while ((b = ins.read(buffer)) > 0) {
				fout.write(buffer, 0, b);
				fout.flush();
				receivedBytes += b;

				publishProgress(receivedBytes);

				if (receivedBytes == intFileSize || isCancelled()) {
					break;
				}
			}

			socket.close();
			fout.close();
		} catch (final Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

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