package demolrucatch.program.com.demolrucatch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.GridView;

public class MainActivity extends AppCompatActivity {
    ImagesAdapter imagesAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GridView gridView=findViewById(R.id.gridView_photo);
        imagesAdapter=new ImagesAdapter(this,0,ImageUrls.imageThumbUrls,gridView);
        gridView.setAdapter(imagesAdapter);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        imagesAdapter.cancelAllTask();


    }
}
