package cn.garymb.ygomobile.utils;

import static cn.garymb.ygomobile.lite.R.string.please_select_target_category;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemLongClickListener;
import com.feihua.dialogutils.util.DialogUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.garymb.ygomobile.AppsSettings;
import cn.garymb.ygomobile.Constants;
import cn.garymb.ygomobile.adapter.TextBaseAdapter;
import cn.garymb.ygomobile.bean.DeckType;
import cn.garymb.ygomobile.bean.events.DeckFile;
import cn.garymb.ygomobile.lite.R;
import cn.garymb.ygomobile.ui.adapters.DeckListAdapter;
import cn.garymb.ygomobile.ui.adapters.TextSelectAdapter;
import cn.garymb.ygomobile.ui.mycard.mcchat.util.ImageUtil;
import cn.garymb.ygomobile.ui.plus.DialogPlus;
import cn.garymb.ygomobile.utils.recyclerview.DeckTypeTouchHelperCallback;

public class YGODialogUtil {
    public static void dialogDeckSelect(Context context, String selectDeckPath, OnDeckMenuListener onDeckMenuListener) {
        ViewHolder viewHolder = new ViewHolder(context, selectDeckPath, onDeckMenuListener);
        viewHolder.show();
    }

    public static ListView dialogl(Context context, String title, String[] list) {
        DialogUtils dialogUtils = DialogUtils.getInstance(context);
        ListView listView = dialogUtils.dialogl1(title
                , new TextBaseAdapter(context, list, YGOUtil.c(R.color.white)
                        , 16, 16, 16, 16));
        dialogUtils.setDialogBackgroundResource(R.drawable.radius);
        dialogUtils.setTitleColor(YGOUtil.c(R.color.holo_blue_light));
        return listView;
    }

    public interface OnDeckMenuListener {
        void onDeckSelect(DeckFile deckFile);

        void onDeckDel(List<DeckFile> deckFileList);

        void onDeckMove(List<DeckFile> deckFileList, DeckType toDeckType);

        void onDeckCopy(List<DeckFile> deckFileList, DeckType toDeckType);

        void onDeckNew(DeckType currentDeckType);

    }

    public interface OnDeckTypeListener {
        void onDeckTypeListener(int position);
    }

    private static class ViewHolder {

        private final int IMAGE_MOVE = 0;
        private final int IMAGE_COPY = 1;
        private final int IMAGE_DEL = 2;

        private final LinearLayout ll_move;
        private final LinearLayout ll_copy;
        private final LinearLayout ll_del;
        private final ImageView iv_move;
        private final ImageView iv_copy;
        private final ImageView iv_del;
        private final TextView tv_move;
        private final TextView tv_copy;
        private final TextView tv_del;
        private final TextSelectAdapter<DeckType> typeAdp;
        private final DeckListAdapter<DeckFile> deckAdp;
        private final Dialog ygoDialog;

        public ViewHolder(Context context, String selectDeckPath, OnDeckMenuListener onDeckMenuListener) {
            DialogUtils du = DialogUtils.getdx(context);
            View viewDialog = du.dialogBottomSheet(R.layout.dialog_deck_select, false);
            RecyclerView rv_type, rv_deck;

            rv_deck = viewDialog.findViewById(R.id.rv_deck);
            rv_type = viewDialog.findViewById(R.id.rv_type);
            ll_move = viewDialog.findViewById(R.id.ll_move);
            ll_copy = viewDialog.findViewById(R.id.ll_copy);
            ll_del = viewDialog.findViewById(R.id.ll_del);
            LinearLayout ll_add = viewDialog.findViewById(R.id.ll_add);
            iv_copy = viewDialog.findViewById(R.id.iv_copy);
            iv_move = viewDialog.findViewById(R.id.iv_move);
            iv_del = viewDialog.findViewById(R.id.iv_del);
            tv_move = viewDialog.findViewById(R.id.tv_move);
            tv_copy = viewDialog.findViewById(R.id.tv_copy);
            tv_del = viewDialog.findViewById(R.id.tv_del);

            hideAllDeckUtil();
            rv_deck.setLayoutManager(new LinearLayoutManager(context));
            rv_type.setLayoutManager(new LinearLayoutManager(context));

            List<DeckType> typeList = DeckUtil.getDeckTypeList(context);

            int typeSelectPosition = 2;
            int deckSelectPosition = -1;
            List<DeckFile> deckList;
            if (!TextUtils.isEmpty(selectDeckPath)) {
                File file = new File(selectDeckPath);
                if (file.exists()) {
                    String name = file.getParentFile().getName();
                    String lastName = file.getParentFile().getParentFile().getName();
                    if (name.equals("pack") || name.equals("cacheDeck")) {
                        //卡包
                        typeSelectPosition = 0;
                    } else if (name.equals("Decks") && lastName.equals(Constants.WINDBOT_PATH)) {
                        //ai卡组
                        typeSelectPosition = 1;
                    } else if (name.equals("deck") && lastName.equals(Constants.PREF_DEF_GAME_DIR)) {
                        //如果是deck并且上一个目录是ygocore的话，保证不会把名字为deck的卡包识别为未分类
                    } else {
                        //其他卡包
                        for (int i = 3; i < typeList.size(); i++) {
                            DeckType deckType = typeList.get(i);
                            if (deckType.getName().equals(name)) {
                                typeSelectPosition = i;
                                break;
                            }
                        }
                    }
                }
            }
            deckList = DeckUtil.getDeckList(typeList.get(typeSelectPosition).getPath());
            if (typeSelectPosition == 0) {
                if (AppsSettings.get().isReadExpansions()) {
                    try {
                        deckList.addAll(DeckUtil.getExpansionsDeckList());
                    } catch (IOException e) {
                        YGOUtil.show("额外卡库加载失败,原因为" + e);
                    }
                }
            }
            typeAdp = new TextSelectAdapter<>(typeList, typeSelectPosition);
            deckAdp = new DeckListAdapter<>(context, deckList, deckSelectPosition);
            rv_type.setAdapter(typeAdp);
            rv_deck.setAdapter(deckAdp);
            typeAdp.setOnItemSelectListener(new TextSelectAdapter.OnItemSelectListener<DeckType>() {
                @Override
                public void onItemSelect(int position, DeckType item) {
                    clearDeckSelect();
                    deckList.clear();
                    deckList.addAll(DeckUtil.getDeckList(item.getPath()));
                    if (position == 0) {
                        if (AppsSettings.get().isReadExpansions()) {
                            try {
                                deckList.addAll(DeckUtil.getExpansionsDeckList());
                            } catch (IOException e) {
                                YGOUtil.show("额外卡库加载失败,原因为" + e);
                            }
                        }
                    }
                    deckAdp.notifyDataSetChanged();
                }
            });
            deckAdp.setOnItemSelectListener(new DeckListAdapter.OnItemSelectListener<DeckFile>() {
                @Override
                public void onItemSelect(int position, DeckFile item) {
                    if (deckAdp.isManySelect()) {
                        deckAdp.addManySelect(item);
                        deckAdp.notifyItemChanged(position);
                    } else {
                        dismiss();
                        onDeckMenuListener.onDeckSelect(item);
                    }
                }
            });
            deckAdp.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(@NonNull BaseQuickAdapter adapter, @NonNull View view, int position) {
                    if (deckAdp.isSelect() || typeAdp.getSelectPosition() == 0)
                        return true;

                    deckAdp.setManySelect(true);
                    if (typeAdp.getSelectPosition() == 1) {
                        showCopyDeckUtil();
                    } else {
                        showAllDeckUtil();
                    }
                    deckAdp.addManySelect((DeckFile) adapter.getItem(position));
                    deckAdp.notifyItemChanged(position);
                    return true;
                }
            });

            ll_add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogl(context, context.getString(R.string.new_deck),
                            new String[]{context.getString(R.string.category_name),
                                    context.getString(R.string.deck_name)}).setOnItemClickListener((parent, view, position, id) -> {
                        du.dis();
                        switch (position) {
                            case 0:
                                //if (deckList.size()>=8){
                                //    YGOUtil.show("最多只能有5个自定义分类");
                                //}
                                DialogPlus builder = new DialogPlus(context);
                                builder.setTitle(R.string.please_input_category_name);
                                EditText editText = new EditText(context);
                                editText.setGravity(Gravity.TOP | Gravity.LEFT);
                                editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                                editText.setSingleLine();
                                builder.setContentView(editText);
                                builder.setOnCloseLinster(DialogInterface::dismiss);
                                builder.setLeftButtonListener((dlg, s) -> {
                                    String name = editText.getText().toString().trim();
                                    if (TextUtils.isEmpty(name)) {
                                        YGOUtil.show(context.getString(R.string.invalid_category_name));
                                        return;
                                    }
                                    File file = new File(AppsSettings.get().getDeckDir(), name);
                                    if (IOUtils.createFolder(file)) {
                                        typeList.add(new DeckType(name, file.getAbsolutePath()));
                                        typeAdp.notifyItemInserted(typeList.size() - 1);
                                        dlg.dismiss();
                                    } else {
                                        YGOUtil.show(context.getString(R.string.create_new_failed));
                                    }

                                });
                                builder.show();
                                break;
                            case 1:
                                onDeckMenuListener.onDeckNew(typeList.get(typeAdp.getSelectPosition()));
                                dismiss();
                                break;
                        }
                    });
                }
            });

            ll_move.setOnClickListener(v -> {
                List<DeckType> otherType = getOtherTypeList();

                dialogl(context, context.getString(please_select_target_category),
                        getStringType(otherType)).setOnItemClickListener((parent, view, position, id) -> {
                    du.dis();
                    DeckType toType = otherType.get(position);
                    IOUtils.createFolder(new File(toType.getPath()));
                    List<DeckFile> deckFileList = deckAdp.getSelectList();
                    for (DeckFile deckFile : deckFileList) {
                        try {
                            FileUtils.moveFile(deckFile.getPath(), new File(toType.getPath(), deckFile.getFileName()).getPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        deckList.remove(deckFile);
                    }
                    YGOUtil.show(context.getString(R.string.done));
                    onDeckMenuListener.onDeckMove(deckAdp.getSelectList(), toType);
                    clearDeckSelect();
                });
            });

            ll_copy.setOnClickListener(v -> {
                List<DeckType> otherType = getOtherTypeList();

                dialogl(context, context.getString(please_select_target_category),
                        getStringType(otherType)).setOnItemClickListener((parent, view, position, id) -> {
                    du.dis();
                    DeckType toType = otherType.get(position);
                    IOUtils.createFolder(new File(toType.getPath()));
                    List<DeckFile> deckFileList = deckAdp.getSelectList();
                    for (DeckFile deckFile : deckFileList) {
                        try {
                            FileUtils.copyFile(deckFile.getPath(), new File(toType.getPath(), deckFile.getFileName()).getPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    YGOUtil.show(context.getString(R.string.done));
                    onDeckMenuListener.onDeckCopy(deckAdp.getSelectList(), toType);
                    clearDeckSelect();
                });
            });

            ll_del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (deckAdp.getSelectList().size() == 0) {
                        YGOUtil.show(context.getString(R.string.no_deck_is_selected));
                        return;
                    }
                    DialogPlus dialogPlus = new DialogPlus(context);
                    dialogPlus.setMessage(R.string.question_delete_deck);
                    dialogPlus.setLeftButtonText(YGOUtil.s(R.string.delete));
                    dialogPlus.setRightButtonText(R.string.Cancel);
                    dialogPlus.setLeftButtonListener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            List<DeckFile> selectDeckList = deckAdp.getSelectList();
                            for (DeckFile deckFile : selectDeckList) {
                                deckFile.getPathFile().delete();
                                deckList.remove(deckFile);
                            }
                            YGOUtil.show(context.getString(R.string.done));
                            dialogPlus.dismiss();
                            onDeckMenuListener.onDeckDel(selectDeckList);
                            clearDeckSelect();
                        }
                    });
                    dialogPlus.setRightButtonListener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialogPlus.dismiss();
                        }
                    });
                    dialogPlus.show();
                }
            });

            ygoDialog = du.getDialog();
            ygoDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    clearDeckSelect();
                }
            });
            ygoDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (deckAdp.isManySelect()) {
                            clearDeckSelect();
                            return true;
                        }

                    }
                    return false;
                }
            });
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new DeckTypeTouchHelperCallback(new OnDeckTypeListener() {
                @Override
                public void onDeckTypeListener(int positon) {
                    File file = new File(typeList.get(positon).getPath());
                    File[] files = file.listFiles();
                    List<DeckFile> deckFileList = new ArrayList<>();
                    if (files != null) {
                        for (File file1 : files) {
                            deckFileList.add(new DeckFile(file1));
                        }
                    }
                    IOUtils.delete(file);
                    YGOUtil.show(context.getString(R.string.done));
                    onDeckMenuListener.onDeckDel(deckFileList);
                    typeAdp.remove(positon);
                    if (typeAdp.getSelectPosition() == positon) {
                        typeAdp.setSelectPosition(2);
                        typeAdp.notifyItemChanged(2);
                    }
                    clearDeckSelect();
                    deckList.clear();
                    deckList.addAll(DeckUtil.getDeckList(typeList.get(2).getPath()));
                    deckAdp.notifyDataSetChanged();
                }
            }));
            itemTouchHelper.attachToRecyclerView(rv_type);
        }

        private String[] getStringType(List<DeckType> deckTypeList) {
            String[] types = new String[deckTypeList.size()];
            for (int i = 0; i < types.length; i++) {
                types[i] = deckTypeList.get(i).getName();
            }
            return types;
        }

        //获取可以移动的分类
        private List<DeckType> getOtherTypeList() {
            List<DeckType> typeList = typeAdp.getData();
            List<DeckType> moveTypeList = new ArrayList<>();
            DeckType selectType = typeList.get(typeAdp.getSelectPosition());
            for (int i = 2; i < typeList.size(); i++) {
                DeckType deckType = typeList.get(i);
                if (!deckType.getPath().equals(selectType.getPath())) {
                    moveTypeList.add(deckType);
                }
            }
            return moveTypeList;
        }

        private void showAllDeckUtil() {
            ImageUtil.reImageColor(IMAGE_MOVE, iv_move);//可用时用原图标色
            ImageUtil.reImageColor(IMAGE_DEL, iv_del);
            ImageUtil.reImageColor(IMAGE_COPY, iv_copy);
            tv_del.setTextColor(YGOUtil.c(R.color.holo_blue_bright));//可用时字色蓝
            tv_copy.setTextColor(YGOUtil.c(R.color.holo_blue_bright));
            tv_move.setTextColor(YGOUtil.c(R.color.holo_blue_bright));
            ll_del.setEnabled(true);
            ll_copy.setEnabled(true);
            ll_move.setEnabled(true);
        }

        private void hideAllDeckUtil() {
            ImageUtil.setGrayImage(IMAGE_MOVE, iv_move);
            ImageUtil.setGrayImage(IMAGE_DEL, iv_del);
            ImageUtil.setGrayImage(IMAGE_COPY, iv_copy);
            tv_del.setTextColor(YGOUtil.c(R.color.star_rank));//不可用时字色灰
            tv_copy.setTextColor(YGOUtil.c(R.color.star_rank));
            tv_move.setTextColor(YGOUtil.c(R.color.star_rank));
            ll_del.setEnabled(false);
            ll_copy.setEnabled(false);
            ll_move.setEnabled(false);
        }

        private void showCopyDeckUtil() {
            ImageUtil.setGrayImage(IMAGE_MOVE, iv_move);
            ImageUtil.setGrayImage(IMAGE_DEL, iv_del);
            ImageUtil.reImageColor(IMAGE_COPY, iv_copy);
            tv_del.setTextColor(YGOUtil.c(R.color.star_rank));
            tv_copy.setTextColor(YGOUtil.c(R.color.holo_blue_bright));
            tv_move.setTextColor(YGOUtil.c(R.color.star_rank));
            ll_del.setEnabled(false);
            ll_copy.setEnabled(true);
            ll_move.setEnabled(false);
        }

        private void clearDeckSelect() {
            deckAdp.setManySelect(false);
            hideAllDeckUtil();
        }

        public void show() {
            if (ygoDialog != null && !ygoDialog.isShowing()) {
                ygoDialog.show();
            }
        }

        public void dismiss() {
            if (ygoDialog != null && ygoDialog.isShowing())
                ygoDialog.dismiss();
        }

    }

}
