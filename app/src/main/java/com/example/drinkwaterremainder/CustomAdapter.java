package com.example.drinkwaterremainder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {
    Context context;
    List<DatabaseHelper.WaterRecord> waterRecords;
    private OnItemDeleteListener deleteListener;
    
    public interface OnItemDeleteListener {
        void onItemDelete(int recordId);
    }
    
    public CustomAdapter(Context context, List<DatabaseHelper.WaterRecord> waterRecords) {
        this.context = context;
        this.waterRecords = waterRecords;
    }
    
    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    public void updateRecords(List<DatabaseHelper.WaterRecord> newRecords) {
        this.waterRecords = newRecords;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public CustomAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.records_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomAdapter.MyViewHolder holder, int position) {
        DatabaseHelper.WaterRecord record = waterRecords.get(position);
        holder.TIME.setText(record.time);
        holder.VALUE.setText(record.amount + " ml");
        
        // Add click listener for delete functionality
        holder.menuButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onItemDelete(record.id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return waterRecords != null ? waterRecords.size() : 0;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView TIME, VALUE;
        ImageView menuButton;
        
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            TIME = itemView.findViewById(R.id.timeID);
            VALUE = itemView.findViewById(R.id.quantityID);
            menuButton = itemView.findViewById(R.id.menuID);
        }
    }
}
