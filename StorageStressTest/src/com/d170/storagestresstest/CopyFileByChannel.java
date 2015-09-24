//import java.io.InputStream;
//import java.io.OutputStream;
//
//import android.util.Log;
//
//// CopyFileTask task = new CopyFileTask();
//// task.execute(srcFile, dstFile);
//
//private class CopyFileTask extends AsyncTask<File, Integer, Boolean> {
//
//        private float speed;
//        // private int progressStatus;
//        private int copyCount;
//        private int fileIndex;
//
//        @Override
//        protected void onPreExecute() {
//            copyCount = Integer.parseInt(mExecutionCountEdTt.getText().toString());
//
//            mProgressDialog.setProgress(0);
//            mProgressDialog.show();
//            mStatusPgBr.setProgress(0);
//        }
//
//        @Override
//        protected void onProgressUpdate(Integer... values) {
//
//            if (values[0] == 0) {
//                // createProgressDialog("Copy " + fileIndex + "-th File (Speed :
//                // " + String.format("%.2f", speed) +
//                // "; total :"+ copyCount + ")");
//                // mProgressDialog.setProgress(values[0]);
//                // mProgressDialog.show();
//
//                if (mStatusPgBr != null) {
//                    mStatusPgBr.setProgress((fileIndex * 100 / copyCount));
//                }
//            }
//            mProgressDialog.setProgress(values[0]);
//
//            // if (mProgressDialog != null) {
//            // mProgressDialog.setProgress(progressStatus);
//            // } else {
//            // mProgressDialog.setProgress(progressStatus);
//            // mProgressDialog.show();
//            // }
//        }
//
//        @Override
//        protected Boolean doInBackground(File... files) {
//            try {
//                for (fileIndex = 1; fileIndex < copyCount + 1; fileIndex++) {
//                    long start = System.nanoTime();
//                    if (fileIndex % 2 == 1) {
//                        copyFile(files[0], files[1]);
//                    } else {
//                        copyFile(files[1], files[0]);
//                    }
//                    publishProgress(mProgressDialog.getMax());
//                    Log.d("Copy a file cost ", "((((( copyFile Cost "
//                            + (double) ((System.nanoTime() - start) / 1000000000.0) + " second. )))))");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return true;
//        }
//
//        @Override
//        protected void onPostExecute(Boolean success) {
//            mProgressDialog.dismiss();
//        }
//
//        private void copyFile(File source, File dest) throws IOException {
//            if (!dest.exists()) {
//                // dest.delete();
//                dest.createNewFile();
//            }
//            InputStream in = null;
//            OutputStream out = null;
//            long start;
//            long end;
//            long total;
//            long lenghtOfFile = source.length();
//            try {
//                in = new FileInputStream(source);
//                out = new FileOutputStream(dest);
//                byte[] buf = new byte[1024];
//                int len = 0;
//                total = 0;
//                start = System.nanoTime();
//                int index = 0;
//                while ((len = in.read(buf)) > 0) {
//                    total += len;
//                    out.write(buf, 0, len);
//                    if (index == 0 || index % 512 == 0) {
//                        end = System.nanoTime();
//                        speed = 1000000000 / (end - start);
//                        start = System.nanoTime();
//                        if (index == 0) {
//                            publishProgress(0);
//                        } else {
//                            publishProgress((int) (total * 100 / lenghtOfFile));
//                        }
//                    }
//                    index++;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                if (in != null) {
//                    in.close();
//                }
//                if (out != null) {
//                    out.close();
//                }
//            }
//        }
//    }