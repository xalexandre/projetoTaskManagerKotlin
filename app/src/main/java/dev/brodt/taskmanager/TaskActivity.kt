package dev.brodt.taskmanager

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dev.brodt.taskmanager.notifications.NotificationHelper
import dev.brodt.taskmanager.notifications.TaskReminderService
import java.util.Calendar


class TaskActivity : AppCompatActivity() {
    private val TAG = "TaskActivity"
    private var uid: String? = null
    private lateinit var db_ref: DatabaseReference
    private var task_id: String = ""

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Iniciando TaskActivity")
        
        // Verificar autenticação primeiro
        uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d(TAG, "Estado de autenticação: ${if (uid != null) "Autenticado com UID: $uid" else "Não autenticado"}")
        
        if (uid == null) {
            // Usuário não autenticado, redirecionar para login
            Log.d(TAG, "Usuário não autenticado, redirecionando para tela de login")
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Importante: encerrar a atividade para evitar retorno
            return
        }
        
        // Inicializar a referência do banco de dados apenas se o usuário estiver autenticado
        db_ref = FirebaseDatabase.getInstance().getReference("users/${uid}/tasks")
        Log.d(TAG, "Referência do banco inicializada: ${db_ref.path}")
        Log.d(TAG, "URL completa do banco: ${db_ref.toString()}")
        
        setContentView(R.layout.activity_task)

        val currentDateTime = Calendar.getInstance()
        val day = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val month = currentDateTime.get(Calendar.MONTH)
        val year = currentDateTime.get(Calendar.YEAR)
        val hour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentDateTime.get(Calendar.MINUTE)

        val title = findViewById<EditText>(R.id.title_input)
        val description = findViewById<EditText>(R.id.description_input)
        val date = findViewById<EditText>(R.id.date_input)
        val time = findViewById<EditText>(R.id.time_input)

        val saveBtn = findViewById<Button>(R.id.save_btn)
        val backButton = findViewById<ImageView>(R.id.back_button)
        val dateIcon = findViewById<ImageView>(R.id.date_icon)
        val timeIcon = findViewById<ImageView>(R.id.time_icon)

        // Verificar se é uma edição ou nova task
        task_id = intent.getStringExtra("taskId") ?: ""
        Log.d(TAG, "Task ID: ${if (task_id.isEmpty()) "Nova task" else task_id}")
        
        loadTask(title, description, date, time)

        saveBtn.setOnClickListener {
            val titleText = title.text.toString();
            val descriptionText = description.text.toString();
            val dateText = date.text.toString();
            val timeText = time.text.toString();

            saveTask(titleText, descriptionText, dateText, timeText)
        }

        backButton.setOnClickListener {
            finish()
        }

        dateIcon.setOnClickListener {
            openDatePicker(date, year, month, day);
        }
        date.setOnClickListener {
            openDatePicker(date, year, month, day);
        }

        timeIcon.setOnClickListener {
            openTimePicker(time, hour, minute)
        }
        time.setOnClickListener {
            openTimePicker(time, hour, minute)
        }
    }

    fun openDatePicker(date: EditText, year: Int, month: Int, day: Int) {
        val datePickerDialog = DatePickerDialog(this, {_, yearOfYear, monthOfYear, dayOfMonth ->
            date.setText(String.format("%02d/%02d/%04d", dayOfMonth, monthOfYear + 1, yearOfYear))
        }, year, month, day);
        datePickerDialog.show();
    }

    fun openTimePicker(time: EditText, hour: Int, minute: Int) {
        val timePickerDialog = TimePickerDialog(this, {_, hourOfDay, minuteOfHour->
            time.setText(String.format("%02d:%02d", hourOfDay, minuteOfHour))
        }, hour, minute, true);
        timePickerDialog.show();
    }

    fun loadTask(title: EditText, description: EditText, date: EditText, time: EditText) {
        if(task_id.isEmpty()) {
            Log.d(TAG, "Nova task, não é necessário carregar dados")
            return
        }

        Log.d(TAG, "Carregando dados da task: $task_id")
        val taskRef = FirebaseDatabase.getInstance().getReference("users/${uid}/tasks/${task_id}")
        Log.d(TAG, "Referência para carregar task: ${taskRef.path}")

        taskRef.addListenerForSingleValueEvent(object: ValueEventListener {
            val ctx = this@TaskActivity;

            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Dados da task recebidos. Existe: ${snapshot.exists()}")
                
                if(!snapshot.exists()) {
                    Log.d(TAG, "Task não encontrada no banco de dados")
                    return
                }

                // Adicionar verificação de null
                val titleValue = snapshot.child("title").value
                val descriptionValue = snapshot.child("description").value
                val dateValue = snapshot.child("date").value
                val timeValue = snapshot.child("time").value
                
                Log.d(TAG, "Dados da task - Título: ${titleValue ?: "null"}, Descrição: ${descriptionValue ?: "null"}, " +
                        "Data: ${dateValue ?: "null"}, Hora: ${timeValue ?: "null"}")
                
                title.setText(titleValue?.toString() ?: "")
                description.setText(descriptionValue?.toString() ?: "")
                date.setText(dateValue?.toString() ?: "")
                time.setText(timeValue?.toString() ?: "")
                
                Log.d(TAG, "Campos preenchidos com sucesso")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao carregar task: ${error.message}", error.toException())
                Toast.makeText(ctx, R.string.error_loading_task, Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Verifica se os dados foram realmente salvos no Firebase
    private fun verifyDataSaved(reference: DatabaseReference, onResult: (Boolean) -> Unit) {
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val exists = snapshot.exists()
                Log.d(TAG, "Verificação de dados salvos: ${if (exists) "Dados encontrados" else "Dados não encontrados"}")
                onResult(exists)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao verificar dados salvos: ${error.message}", error.toException())
                onResult(false)
            }
        })
    }

    fun saveTask(title: String, description: String, date: String, time: String) {
        Log.d(TAG, "Salvando task - Título: $title, Descrição: $description, Data: $date, Hora: $time")
        
        // Continuar com o salvamento mesmo offline (Firebase cuidará da sincronização quando online)
        if(task_id.isEmpty()) {
            Log.d(TAG, "Criando nova task")
            val task = hashMapOf(
                "title" to title,
                "description" to description,
                "date" to date,
                "time" to time
            )

            val newRef = db_ref.push()
            val newKey = newRef.key
            Log.d(TAG, "Nova referência gerada: $newKey")
            Log.d(TAG, "Caminho completo para nova task: ${newRef.toString()}")
            
            if (newKey == null) {
                Log.e(TAG, "Erro: Não foi possível gerar uma chave para a nova task")
                Toast.makeText(this, "Erro ao gerar referência para a task", Toast.LENGTH_SHORT).show()
                return
            }
            
            newRef.setValue(task)
                .addOnSuccessListener {
                    Log.d(TAG, "Task salva com sucesso no Firebase")
                    Toast.makeText(this, R.string.task_saved, Toast.LENGTH_SHORT).show()
                    
                    // Show notification
                    val notificationHelper = NotificationHelper(this)
                    notificationHelper.showTaskCreatedNotification(newKey, title)
                    
                    // Log event to Firebase Analytics
                    val app = application as TaskManagerApplication
                    val params = android.os.Bundle().apply {
                        putString("task_id", newKey)
                        putString("task_title", title)
                    }
                    app.logEvent("task_created", params)
                    
                    // Start the reminder service
                    val serviceIntent = Intent(this, TaskReminderService::class.java)
                    startService(serviceIntent)
                    
                    // Verificar se os dados foram realmente salvos após um breve delay
                    android.os.Handler().postDelayed({
                        verifyDataSaved(newRef) { saved ->
                            if (saved) {
                                Log.d(TAG, "Verificação confirmou que a task foi salva com sucesso")
                            } else {
                                Log.e(TAG, "Verificação falhou: A task não foi encontrada no banco de dados")
                                runOnUiThread {
                                    AlertDialog.Builder(this)
                                        .setTitle("Aviso")
                                        .setMessage("A task pode não ter sido salva corretamente. Verifique sua conexão e tente novamente.")
                                        .setPositiveButton("OK", null)
                                        .show()
                                }
                            }
                        }
                    }, 2000) // Espera 2 segundos antes de verificar
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erro ao salvar task: ${e.message}", e)
                    Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
                    
                    AlertDialog.Builder(this)
                        .setTitle("Erro ao Salvar")
                        .setMessage("Ocorreu um erro ao salvar a task: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
                .addOnCompleteListener { task ->
                    Log.d(TAG, "Operação de salvamento completa. Sucesso: ${task.isSuccessful}")
                    if (!task.isSuccessful) {
                        Log.e(TAG, "Falha na operação: ${task.exception?.message}", task.exception)
                    }
                }
        } else {
            Log.d(TAG, "Atualizando task existente: $task_id")
            val taskRef = FirebaseDatabase.getInstance().getReference("users/${uid}/tasks/${task_id}")
            
            taskRef.addListenerForSingleValueEvent(object: ValueEventListener {
                val ctx = this@TaskActivity;

                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "Dados da task recebidos para atualização. Existe: ${snapshot.exists()}")
                    
                    if(!snapshot.exists()) {
                        Log.d(TAG, "Task não encontrada para atualização")
                        runOnUiThread {
                            AlertDialog.Builder(ctx)
                                .setTitle("Erro")
                                .setMessage("A task que você está tentando atualizar não foi encontrada.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                        return
                    }

                    try {
                        val task = snapshot.value as? HashMap<String, String> ?: HashMap()
                        Log.d(TAG, "Dados atuais: $task")

                        task["title"] = title
                        task["description"] = description
                        task["date"] = date
                        task["time"] = time
                        
                        taskRef.setValue(task)
                            .addOnSuccessListener {
                                Log.d(TAG, "Task atualizada com sucesso")
                                Toast.makeText(ctx, R.string.task_edited, Toast.LENGTH_SHORT).show()
                                
                                // Verificar se os dados foram realmente atualizados
                                android.os.Handler().postDelayed({
                                    verifyDataSaved(taskRef) { saved ->
                                        if (saved) {
                                            Log.d(TAG, "Verificação confirmou que a task foi atualizada com sucesso")
                                        } else {
                                            Log.e(TAG, "Verificação falhou: A task não foi encontrada após atualização")
                                            runOnUiThread {
                                                AlertDialog.Builder(ctx)
                                                    .setTitle("Aviso")
                                                    .setMessage("A task pode não ter sido atualizada corretamente. Verifique sua conexão e tente novamente.")
                                                    .setPositiveButton("OK", null)
                                                    .show()
                                            }
                                        }
                                    }
                                }, 2000) // Espera 2 segundos antes de verificar
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Erro ao atualizar task: ${e.message}", e)
                                Toast.makeText(ctx, "Erro ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                                
                                runOnUiThread {
                                    AlertDialog.Builder(ctx)
                                        .setTitle("Erro ao Atualizar")
                                        .setMessage("Ocorreu um erro ao atualizar a task: ${e.message}")
                                        .setPositiveButton("OK", null)
                                        .show()
                                }
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exceção ao processar dados da task: ${e.message}", e)
                        Toast.makeText(ctx, R.string.error_task_edited, Toast.LENGTH_SHORT).show()
                        
                        runOnUiThread {
                            AlertDialog.Builder(ctx)
                                .setTitle("Erro")
                                .setMessage("Ocorreu um erro ao processar os dados da task: ${e.message}")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Operação cancelada: ${error.message}", error.toException())
                    Toast.makeText(ctx, R.string.error_task_edited, Toast.LENGTH_SHORT).show()
                    
                    runOnUiThread {
                        AlertDialog.Builder(ctx)
                            .setTitle("Operação Cancelada")
                            .setMessage("A operação foi cancelada: ${error.message}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            })
        }
        
        // Perguntar ao usuário se deseja fechar a tela após salvar
        AlertDialog.Builder(this)
            .setTitle("Task Salva")
            .setMessage("Deseja voltar para a tela principal?")
            .setPositiveButton("Sim") { _, _ ->
                Log.d(TAG, "Usuário optou por fechar a tela")
                finish()
            }
            .setNegativeButton("Não") { _, _ ->
                Log.d(TAG, "Usuário optou por permanecer na tela")
            }
            .show()
    }
}
