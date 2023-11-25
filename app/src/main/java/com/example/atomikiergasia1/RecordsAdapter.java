package com.example.atomikiergasia1;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.ViewHolder> {

    private List<String> records;
    private Context context;

    public RecordsAdapter(Context context, List<String> records) {
        this.records = records;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.record_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String record = records.get(position);
        holder.recordTextView.setText(record);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView recordTextView;

        public ViewHolder(View view) {
            super(view);
            recordTextView = view.findViewById(R.id.recordTextView);
        }
    }
}

