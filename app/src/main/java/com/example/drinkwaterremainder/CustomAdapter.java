package com.example.drinkwaterremainder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {
    Context context;
    ArrayList<String> id, time, value;
    private OnItemDeleteListener deleteListener;
    
    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }
    
    public CustomAdapter(Context context, ArrayList<String> id, ArrayList<String> time, ArrayList<String> value) {
        this.id = id;
        this.time = time;
        this.value = value;
        this.context = context;
    }
    
    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
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
        holder.TIME.setText(time.get(position));
        holder.VALUE.setText(value.get(position));
        
        // Add click listener for delete functionality
        holder.menuButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onItemDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return id.size();
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
