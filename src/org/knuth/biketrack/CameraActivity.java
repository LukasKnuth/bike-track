package org.knuth.biketrack;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import org.knuth.biketrack.camera.CameraPreview;

/**
 * <p>The Activity for taking photos wile on a Tour.</p>
 * <p>This uses the Camera API, because the Intent API sucks.</p>
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class CameraActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener{

    private CameraPreview preview;
    private Button trigger;
    private Button flash;
    private SeekBar zoom;

    @Override
    public void onCreate(Bundle saved){
        super.onCreate(saved);
        this.setContentView(R.layout.camera);
        // Load the Camera:
        this.preview = new CameraPreview(this);
        // Place it in the Layout:
        SurfaceView prev = (SurfaceView)this.findViewById(R.id.camera_preview);
        ViewGroup parent = (ViewGroup) prev.getParent();
        int index = parent.indexOfChild(prev);
        parent.removeView(prev);
        parent.addView(preview, index);
        // Get the controls:
        trigger = (Button) this.findViewById(R.id.camera_trigger);
        flash = (Button) this.findViewById(R.id.camera_flash);
        zoom = (SeekBar) this.findViewById(R.id.camera_zoom_control);
    }

    @Override
    public void onResume(){
        super.onResume();
        zoom.setOnSeekBarChangeListener(this);
        zoom.setMax(preview.getParameters().getMaxZoom());
        zoom.setProgress(preview.getParameters().getZoom());

    }

    /** ------- ZOOM Controls ------------ */

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) return;
        preview.getParameters().setZoom(progress);
        preview.updateCamera();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private class CameraLoader extends AsyncTask<Void, Void, CameraPreview>{

        @Override
        protected CameraPreview doInBackground(Void... voids) {
            return new CameraPreview(CameraActivity.this);
        }

        @Override
        protected void onPostExecute (CameraPreview result){
            if (result != null){
                CameraActivity.this.setContentView(result);
            }
            Log.v(Main.LOG_TAG, "Got: "+result);
        }
    }

}
