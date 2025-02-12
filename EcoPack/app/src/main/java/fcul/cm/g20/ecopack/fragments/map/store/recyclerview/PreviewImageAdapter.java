package fcul.cm.g20.ecopack.fragments.map.store.recyclerview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import fcul.cm.g20.ecopack.R;
import fcul.cm.g20.ecopack.fragments.map.UpdateImageThreshold;

public class PreviewImageAdapter extends RecyclerView.Adapter<PreviewImageAdapter.PreviewImageViewHolder> {
    private Context context;
    private ArrayList<String> photos;
    private UpdateImageThreshold updateImageThreshold;

    public PreviewImageAdapter(Context context, ArrayList<String> photos, UpdateImageThreshold updateImageThreshold) {
        this.photos = photos;
        this.context = context;
        this.updateImageThreshold = updateImageThreshold;
    }

    @NonNull
    @Override
    public PreviewImageAdapter.PreviewImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_picture, parent, false);
        return new PreviewImageAdapter.PreviewImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreviewImageAdapter.PreviewImageViewHolder holder, final int position) {
        byte[] pictureArray = android.util.Base64.decode(photos.get(position), android.util.Base64.DEFAULT);
        Bitmap pictureBitmap = BitmapFactory.decodeByteArray(pictureArray, 0, pictureArray.length);
        holder.getPhoto().setImageBitmap(pictureBitmap);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public class PreviewImageViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        ImageButton delete;

        public PreviewImageViewHolder(@NonNull View itemView) {
            super(itemView);

            photo = itemView.findViewById(R.id.item_picture);
            delete = itemView.findViewById(R.id.item_delete);

            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    byte[] photoByteArray = android.util.Base64.decode(photos.get(getAdapterPosition()), android.util.Base64.DEFAULT);
                    updateImageThreshold.update(photoByteArray.length);
                    photos.remove(getAdapterPosition());
                    notifyItemRemoved(getAdapterPosition());
                }
            });
        }

        public ImageView getPhoto() {
            return photo;
        }

        public ImageButton getDelete() {
            return delete;
        }
    }
}