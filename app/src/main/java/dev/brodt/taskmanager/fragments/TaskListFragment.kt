package dev.brodt.taskmanager.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import dev.brodt.taskmanager.base.BaseFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dev.brodt.taskmanager.R
import dev.brodt.taskmanager.Task
import dev.brodt.taskmanager.TaskActivity

/**
 * A fragment that displays a list of tasks.
 * This fragment handles loading, displaying, editing, and deleting tasks.
 */
class TaskListFragment : BaseFragment() {
    private val TAG = "TaskListFragment"
    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    
    private lateinit var db_ref: DatabaseReference
    private val taskList = ArrayList<Task>()
    private val taskKeys = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>
    
    private var onTaskClickListener: OnTaskClickListener? = null
    
    interface OnTaskClickListener {
        fun onTaskClick(taskId: String)
        fun onTaskLongClick(taskId: String)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_task_list, container, false)
        
        listView = view.findViewById(R.id.tasks)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyView = view.findViewById(R.id.empty_view)
        
        // Initialize Firebase reference
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            db_ref = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/tasks")
            
            // Configure adapter
            val displayItems = ArrayList<String>()
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayItems)
            listView.adapter = adapter
            listView.emptyView = emptyView
            
            // Set up listeners
            setupListItemListeners()
            
            // Load data
            loadData()
        } else {
            Log.e(TAG, "User not authenticated")
            emptyView.text = "User not authenticated"
        }
        
        return view
    }

    /**
     * Load tasks from Firebase.
     */
    fun loadData() {
        Log.d(TAG, "Iniciando carregamento de dados do Firebase")
        Log.d(TAG, "Referência do banco: ${db_ref.path}")
        
        // Mostrar indicador de carregamento
        progressBar.visibility = View.VISIBLE
        emptyView.text = getString(R.string.loading_tasks)
        
        db_ref.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Dados recebidos do Firebase. Existe: ${snapshot.exists()}, Contagem de filhos: ${snapshot.childrenCount}")
                
                // Limpar listas
                taskList.clear()
                taskKeys.clear()
                adapter.clear()
                
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    Log.d(TAG, "Nenhuma task encontrada")
                    emptyView.text = getString(R.string.no_tasks)
                    progressBar.visibility = View.GONE
                    return
                }

                for(element in snapshot.children) {
                    try {
                        val key = element.key ?: continue
                        
                        // Extrair dados da task
                        val title = element.child("title").getValue(String::class.java) ?: "Sem título"
                        val description = element.child("description").getValue(String::class.java) ?: ""
                        val date = element.child("date").getValue(String::class.java) ?: ""
                        val time = element.child("time").getValue(String::class.java) ?: ""
                        
                        Log.d(TAG, "Task encontrada - Key: $key, Título: $title, Data: $date, Hora: $time")
                        
                        // Criar objeto Task
                        val task = Task(key, title, description, date, time)
                        taskList.add(task)
                        taskKeys.add(key)
                        
                        // Formatar exibição da task
                        val displayText = if (date.isNotEmpty() && time.isNotEmpty()) {
                            "$title\n$date - $time"
                        } else if (date.isNotEmpty()) {
                            "$title\n$date"
                        } else {
                            title
                        }
                        
                        adapter.add(displayText)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao processar task: ${e.message}", e)
                    }
                }
                
                Log.d(TAG, "Total de tasks carregadas: ${taskList.size}")
                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao carregar tasks: ${error.message}", error.toException())
                Toast.makeText(context, R.string.error_loading_tasks, Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                emptyView.text = "Erro ao carregar tarefas"
            }
        })
    }

    /**
     * Set up listeners for list items.
     */
    private fun setupListItemListeners() {
        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (position >= 0 && position < taskKeys.size) {
                val taskId = taskKeys[position]
                
                if (onTaskClickListener != null) {
                    onTaskClickListener?.onTaskLongClick(taskId)
                } else {
                    // Default implementation if no listener is set
                    showDeleteDialog(taskId)
                }
            }
            true
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            if (position >= 0 && position < taskKeys.size) {
                val taskId = taskKeys[position]
                
                if (onTaskClickListener != null) {
                    onTaskClickListener?.onTaskClick(taskId)
                } else {
                    // Default implementation if no listener is set
                    val activity = Intent(requireContext(), TaskActivity::class.java)
                    activity.putExtra("taskId", taskId)
                    startActivity(activity)
                }
            }
        }
    }

    /**
     * Show a dialog to confirm task deletion.
     *
     * @param taskId The ID of the task to delete.
     */
    private fun showDeleteDialog(taskId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_delete_task)
            .setMessage(R.string.message_delete_task)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                db_ref.child(taskId).removeValue()
                    .addOnSuccessListener {
                        Log.d(TAG, "Task excluída com sucesso: $taskId")
                        Toast.makeText(context, R.string.task_deleted, Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erro ao excluir task: ${e.message}", e)
                        Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Set a listener to be notified when a task is clicked or long-clicked.
     *
     * @param listener The listener to be notified.
     */
    fun setOnTaskClickListener(listener: OnTaskClickListener) {
        onTaskClickListener = listener
    }

    /**
     * Refresh the task list.
     */
    fun refresh() {
        loadData()
    }
}
