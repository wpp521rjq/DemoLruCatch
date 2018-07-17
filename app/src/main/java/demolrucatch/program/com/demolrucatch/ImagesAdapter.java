package demolrucatch.program.com.demolrucatch;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImagesAdapter extends ArrayAdapter<String>  implements AbsListView.OnScrollListener{


    /**
     * set集合庸才存储bitmap 下载任务
     * set 集合和list集合的区别，set集合是无序的，list集合是有序的，
     * set集合是不允许重复的，list集合是允许重复的
     * 在性能上来说，list集合是远高于set集合
     */
    private Set<BitmapWorkTask> taskCollection;

    /**
     * 图片缓存的类，用于缓存从网络上下载的图片，以bitmap的形势存储起来
     *
     */
    private LruCache<String ,Bitmap> mMemoryCache;


    /**
     * 图片布局view
     */
    private GridView mGridView;

    /**
     * 第一张可见图片的下坐标
     *
     */
    private int mFirstVisiableItem;

    /**
     * 可见图片的数量
     */
    private int mVisiableItemCount;


    /**
     * 是否是第一次进入
     */
    private boolean isFirstEnter=true;

    private Context mContext;


    public ImagesAdapter(@NonNull Context context, int textViewResourceId, @NonNull String[] objects,GridView gridView) {
        super(context, textViewResourceId, objects);

        //初始化
        mContext=context;
        mGridView=gridView;
        taskCollection=new HashSet<>();
        int maxMemory= (int) Runtime.getRuntime().maxMemory();
        int cacheSize=maxMemory/8;


        mMemoryCache=new LruCache<String, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        mGridView.setOnScrollListener(this);
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String url=getItem(position);
        ImageViewHolder imageViewHolder=null;
        if(convertView==null){
            imageViewHolder=new ImageViewHolder();
            convertView=LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_layout,parent,false);
            imageViewHolder.imageView=convertView.findViewById(R.id.photo);
            imageViewHolder.imageView.setTag(url);
            convertView.setTag(imageViewHolder);
        }else{
            imageViewHolder= (ImageViewHolder) convertView.getTag();
        }
        setImageView(url,imageViewHolder.imageView);
        return convertView;
    }

    private void setImageView(String url, ImageView imageView) {
        Bitmap bitmap=getBitmapFromMemoryCache(url);
        if(bitmap==null){
            imageView.setImageBitmap(bitmap);
        }else{
            imageView.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    class ImageViewHolder{
        ImageView imageView;
    }



    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //在gridview滑动的时候取消所有正在下载的任务,在停止的时候下载任务
        if(scrollState==SCROLL_STATE_IDLE){//滑动停止
        loadImageBitmaps(mFirstVisiableItem,mVisiableItemCount);
        }else{//正在滑动
            cancelAllTask();
        }
    }


    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        //在滑动的时候
        mFirstVisiableItem=firstVisibleItem;
        mVisiableItemCount=visibleItemCount;
        //因为加载图片的任务在onScrollStateChanaged中进行的，所以在这个地方就不进行图片的加载了，但是首次进入的话，不会进行加载图片，所以在这个地方还需要进行加载，
        //在首次进入的时候执行下载任务

        if(isFirstEnter&&visibleItemCount>0){
            loadImageBitmaps(firstVisibleItem,mVisiableItemCount);
            isFirstEnter=false;
        }
    }

    public void cancelAllTask() {
        //取消所有正在下载的任务
        if(taskCollection!=null){
            for (BitmapWorkTask task :
                    taskCollection) {
            //进行取消
            task.cancel(false);
            }
        }
    }


    /**
     * 在滑动停止的时候加载图片
     * @param mFirstVisiableItem
     * @param mVisiableItemCount
     */
    private void loadImageBitmaps(int mFirstVisiableItem, int mVisiableItemCount) {
        for(int i=mFirstVisiableItem;i<mFirstVisiableItem+mVisiableItemCount;i++){
            String imageUrl=ImageUrls.imageThumbUrls[i];
            Bitmap bitmap=getBitmapFromMemoryCache(imageUrl);
            if(bitmap==null){
                BitmapWorkTask bitmapWorkTask=new BitmapWorkTask();
                taskCollection.add(bitmapWorkTask);
                bitmapWorkTask.execute(imageUrl);
            }else{
                //如果已经有的话，
                ImageView imageView=mGridView.findViewWithTag(imageUrl);
                if(imageView!=null&bitmap!=null){
                    imageView.setImageBitmap(bitmap);
                }

            }
        }
    }




    /**
     * 第一个参数是传入参数，
     * 第二个参数是指progress，表示任务进行时候的进度
     * 第三个参数是指返回结果
     */
    private class BitmapWorkTask extends AsyncTask<String ,Void ,Bitmap>{


        /**
         * 在进行异步加载的时候图片的url地址
         */
        private String imageUrl;

        @Override
        protected Bitmap doInBackground(String... strings) {

            imageUrl=strings[0];
            //在后台开始下载bitmap
            Bitmap bitmap=downLoadBitmap(imageUrl);

            //图片下载完成之后缓存到Lrycache中
            if(bitmap!=null){
                addBitmapToMemaryCache(imageUrl,bitmap);
            }
            return bitmap;
        }


        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //在下载之后将bitmap 找到对应的imageview控件将其显示出来
            ImageView imageView= mGridView.findViewWithTag(imageUrl);
            if(imageView!=null&bitmap!=null){
                //设置一张默认图
                imageView.setImageBitmap(bitmap);
            }
            //将图片显示出来之后，将异步下载任务进行移除
            taskCollection.remove(this);
        }

        /**
         * 得到bitmap
         * @param imageUrl
         * @return
         */
        private Bitmap downLoadBitmap(String imageUrl) {
            //进行下载bitmap
            Bitmap bitmap=null;
            HttpURLConnection httpURLConnection=null;
            try {
                URL url=new URL(imageUrl);
                httpURLConnection= (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();
                httpURLConnection.getResponseCode();
                bitmap=BitmapFactory.decodeStream(httpURLConnection.getInputStream());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(httpURLConnection!=null){
                    httpURLConnection.disconnect();
                }
            }
            return bitmap;
        }
    }


    /**
     * 将下载下来的bitmap添加到cache中，
     * @param key
     * @param bitmap
     */
    private void addBitmapToMemaryCache(String key,Bitmap bitmap) {
        //先从内存中取，如果没有则添加
        if(getBitmapFromMemoryCache(key)==null) {
            mMemoryCache.put(key, bitmap);
        }
    }


    private Bitmap getBitmapFromMemoryCache(String key){
        return mMemoryCache.get(key);
    }
}
