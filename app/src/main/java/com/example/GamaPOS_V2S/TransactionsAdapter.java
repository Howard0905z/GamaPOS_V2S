package com.example.GamaPOS_V2S;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder> {

    private List<TransactionRecord> transactionList;
    private OnTransactionClickListener listener;

    public TransactionsAdapter(List<TransactionRecord> transactionList, OnTransactionClickListener listener) {
        this.transactionList = transactionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionRecord transaction = transactionList.get(position);
        holder.textViewInvoiceNumber.setText(transaction.getInvoiceNumber());
        holder.textViewInvoiceAmount.setText(String.valueOf(transaction.getInvoiceAmount()));
        holder.textViewTransactionDate.setText(transaction.getTransactionDate());

        holder.itemView.setOnClickListener(v -> listener.onTransactionClick(transaction));

        if (transaction.getInvoiceStatus().equals("已作廢")) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFDDDD")); // 例如，红色背景表示作废
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF")); // 正常白色背景
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView textViewInvoiceNumber, textViewInvoiceAmount, textViewTransactionDate;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewInvoiceNumber = itemView.findViewById(R.id.textViewInvoiceNumber);
            textViewInvoiceAmount = itemView.findViewById(R.id.textViewInvoiceAmount);
            textViewTransactionDate = itemView.findViewById(R.id.textViewTransactionDate);
        }
    }

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionRecord transaction);
    }
}
