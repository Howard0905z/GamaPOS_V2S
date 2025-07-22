package com.example.EcoPOS_V2S.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.EcoPOS_V2S.R;
import com.example.EcoPOS_V2S.models.CartItem;
import com.example.EcoPOS_V2S.models.Addon;


public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private ArrayList<CartItem> cartItems;
    private double maxPercentageDiscount;
    private double maxFixedDiscount;
    private double discountAmount;
    private double rebateAmount;

    public CartAdapter(Context context, ArrayList<CartItem> cartItems, double maxPercentageDiscount, double maxFixedDiscount) {
        this.context = context;
        this.cartItems = cartItems;
        this.maxPercentageDiscount = maxPercentageDiscount;
        this.maxFixedDiscount = maxFixedDiscount;
        this.discountAmount = 0;
        this.rebateAmount = 0;
    }

    public void setMaxPercentageDiscount(double maxPercentageDiscount) {
        this.maxPercentageDiscount = maxPercentageDiscount;
    }

    public void setMaxFixedDiscount(double maxFixedDiscount) {
        this.maxFixedDiscount = maxFixedDiscount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getRebateAmount() {
        return rebateAmount;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        try {
            CartItem cartItem = cartItems.get(position);
            holder.textViewName.setText(cartItem.getName());

            // 设置显示的原价
            holder.textViewPrice.setText(String.format(Locale.getDefault(), "$%.2f", cartItem.getOriginalPrice()));
            holder.textViewQuantity.setText(String.format(Locale.getDefault(), "x%d", cartItem.getQuantity()));
            holder.checkBox.setOnCheckedChangeListener(null); // 先取消监听器，避免影响复用

            // 显示折扣后的价格
            if (cartItem.getOriginalPrice() > cartItem.getPrice()) {
                holder.textViewDiscountedPrice.setText(String.format(Locale.getDefault(), "$%.2f", cartItem.getPrice())); // 显示折扣后的价格
                holder.textViewPrice.setPaintFlags(holder.textViewPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // 原价显示删除线
                holder.textViewDiscountedPrice.setVisibility(View.VISIBLE);
            } else {
                holder.textViewPrice.setPaintFlags(holder.textViewPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.textViewDiscountedPrice.setVisibility(View.GONE);
            }

            // 设置 CheckBox 的状态和背景颜色
            holder.checkBox.setChecked(cartItem.isSelected());
            holder.itemView.setBackgroundColor(cartItem.isSelected() ? Color.LTGRAY : Color.TRANSPARENT);

            // 设置 CheckBox 的监听器
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                cartItem.setSelected(isChecked);
                holder.itemView.setBackgroundColor(cartItem.isSelected() ? Color.LTGRAY : Color.TRANSPARENT);
                Log.d("CartAdapter", "Checkbox changed: " + cartItem.getName() + ", Selected: " + cartItem.isSelected());
            });

            // 显示 addon 信息
            List<Addon> addons = cartItem.getAddons();
            if (addons != null && !addons.isEmpty()) {
                StringBuilder addonDetails = new StringBuilder();
                for (Addon addon : addons) {
                    addonDetails.append(addon.getAddonCategoryName()).append(": ").append(addon.getName()).append("\n");
                }
                holder.textViewAddons.setText(addonDetails.toString());
            } else {
                holder.textViewAddons.setText("No Addons");
            }

            // 点击事件处理
            holder.itemView.setOnClickListener(v -> {
                cartItem.setSelected(!cartItem.isSelected());
                holder.itemView.setBackgroundColor(cartItem.isSelected() ? Color.LTGRAY : Color.TRANSPARENT);
                holder.checkBox.setChecked(cartItem.isSelected()); // 同步更新 CheckBox 的状态
                Log.d("CartAdapter", "Item clicked: " + cartItem.getName() + ", Selected: " + cartItem.isSelected());
            });

        } catch (Exception e) {
            Log.e("CartAdapter", "Error in onBindViewHolder: " + e.getMessage(), e);
        }
    }



    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void applyDiscount(double discountPercentage) {
        try {
            Log.d("CartAdapter", "applyDiscount called with percentage: " + discountPercentage);
            if (discountPercentage > maxPercentageDiscount) {
                Toast.makeText(context, "折扣不能超過最大百分比: " + maxPercentageDiscount + "%", Toast.LENGTH_LONG).show();
                return;
            }

            discountAmount = 0;
            for (CartItem item : cartItems) {
                if (item.isSelected()) {
                    double originalPrice = item.getOriginalPrice(); // 获取原价
                    if (originalPrice > 0) { // 确保原价大于0
                        double discountedPrice = originalPrice * (1 - discountPercentage / 100); // 计算折扣价格
                        item.setPrice(discountedPrice); // 更新当前的价格
                        item.setTotalPrice(discountedPrice * item.getQuantity()); // 更新总价
                        discountAmount += (originalPrice * item.getQuantity()) - item.getTotalPrice(); // 计算折扣金额

                        Log.d("CartAdapter", "Item: " + item.getName() + ", Original Price: " + originalPrice + ", Discounted Price: " + discountedPrice + ", Quantity: " + item.getQuantity() + ", Discount Amount: " + discountAmount);
                    } else {
                        Log.e("CartAdapter", "Original price is invalid for item: " + item.getName());
                    }
                }
            }
            Log.d("CartAdapter", "Total Discount applied: " + discountAmount);
            notifyDataSetChanged();
        } catch (Exception e) {
            Log.e("CartAdapter", "Error in applyDiscount: " + e.getMessage(), e);
        }
    }




    public void applyRebate(double rebateAmountValue) {
        try {
            Log.d("CartAdapter", "applyRebate called with amount: " + rebateAmountValue);
            if (rebateAmountValue > maxFixedDiscount) {
                Toast.makeText(context, "折讓不能超過最大固定金額: $" + maxFixedDiscount, Toast.LENGTH_LONG).show();
                return;
            }

            rebateAmount = 0;
            for (CartItem item : cartItems) {
                if (item.isSelected()) {
                    double originalTotalPrice = item.getPrice() * item.getQuantity();
                    double discountedPrice = Math.ceil(item.getPrice() - rebateAmountValue); // 计算折让价格
                    item.setDiscountedPrice(discountedPrice);
                    item.setTotalPrice(discountedPrice * item.getQuantity());
                    rebateAmount += originalTotalPrice - item.getTotalPrice(); // 累加每个商品的折让金额

                    // 更新原价为折扣价
                    item.setPrice(discountedPrice);

                    Log.d("CartAdapter", "Item: " + item.getName() + ", Original Price: " + item.getPrice() + ", Rebate Amount: " + rebateAmountValue + ", Quantity: " + item.getQuantity() + ", Rebate Applied: " + (originalTotalPrice - item.getTotalPrice()));
                }
            }
            Log.d("CartAdapter", "Rebate applied: " + rebateAmount);
            notifyDataSetChanged();
        } catch (Exception e) {
            Log.e("CartAdapter", "Error in applyRebate: " + e.getMessage(), e);
        }
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewPrice, textViewQuantity, textViewDiscountedPrice, textViewAddons;
        CheckBox checkBox;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewPrice = itemView.findViewById(R.id.textViewPrice);
            textViewQuantity = itemView.findViewById(R.id.textViewQuantity);
            textViewDiscountedPrice = itemView.findViewById(R.id.textViewDiscountedPrice);
            textViewAddons = itemView.findViewById(R.id.textViewAddons);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}

