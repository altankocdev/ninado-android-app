package com.altankoc.nidado.ui.listdetail

import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.TodoList

data class ListDetailUiState(
    val todoList: TodoList? = null,
    val items: List<ListItem> = emptyList(),
    val isLoading: Boolean = true,
    val currentTime: Long = System.currentTimeMillis(),

    // Liste adı düzenleme
    val isEditingListTitle: Boolean = false,
    val listTitleInput: String = "",
    val listTitleError: String? = null,

    // Görev adı düzenleme (hangi görev düzenleniyor)
    val editingItemId: Int? = null,
    val itemTitleInput: String = "",
    val itemTitleError: String? = null,

    // Saat seçici (hangi görev için açık)
    val timePickerItemId: Int? = null,

    // Miktar düzenleme (Shopping için)
    val editingQuantityItemId: Int? = null,
    val quantityInput: String = "",

    // Çoklu seçim modu
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Int> = emptySet(),

    // Yeni görev ekleme
    val isAddingTask: Boolean = false,
    val newTaskTitle: String = "",
    val newTaskTime: Long? = null,
    val newTaskQuantity: String = "",

    // Dialoglar
    val showDeleteListDialog: Boolean = false,
    val showDeleteSelectedDialog: Boolean = false
)