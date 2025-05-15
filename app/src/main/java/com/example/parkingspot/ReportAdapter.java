package com.example.parkingspot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> reportList;  // Μπορεί να περιέχει τόσο ReportItem όσο και TicketItem

    public ReportAdapter(List<Object> reportList) {
        this.reportList = reportList;
    }

    @Override
    public int getItemViewType(int position) {
        if (reportList.get(position) instanceof ReportItem) {
            return TYPE_HEADER;
        } else {
            return TYPE_ITEM;
        }
    }

    public void updateData(List<Object> newReportList) {
        this.reportList = new ArrayList<>(newReportList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
            return new ReportViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
            return new TicketViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ReportViewHolder) {
            ReportItem reportItem = (ReportItem) reportList.get(position);
            ((ReportViewHolder) holder).operatorTextView.setText("Χειριστής: " + reportItem.getOperator());
            ((ReportViewHolder) holder).countTextView.setText("Σύνολο εισιτηρίων: " + reportItem.getTicketCount());
        } else {
            TicketItem ticketItem = (TicketItem) reportList.get(position);
            ((TicketViewHolder) holder).plateTextView.setText("Πινακίδα: " + ticketItem.getPlate());
            ((TicketViewHolder) holder).datetimeTextView.setText("Ημερομηνία: " + ticketItem.getDatetime());
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    // ViewHolder για τις συνολικές αναφορές
    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView operatorTextView, countTextView;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            operatorTextView = itemView.findViewById(R.id.text_operator);
            countTextView = itemView.findViewById(R.id.text_ticket_count);
        }
    }

    // ViewHolder για τα μεμονωμένα εισιτήρια
    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView plateTextView, datetimeTextView;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            plateTextView = itemView.findViewById(R.id.text_plate);
            datetimeTextView = itemView.findViewById(R.id.text_datetime);
        }
    }
}
