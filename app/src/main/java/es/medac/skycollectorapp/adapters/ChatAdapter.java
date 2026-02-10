// Declaración del paquete que contiene la lógica de los adaptadores
package es.medac.skycollectorapp.adapters;

// Importación para la manipulación de colores en formato hexadecimal o constantes
import android.graphics.Color;
// Importación para definir el posicionamiento y alineación de los elementos
import android.view.Gravity;
// Importación para convertir archivos de diseño XML en objetos de vista Java
import android.view.LayoutInflater;
// Importación de la clase base para todos los componentes de la interfaz
import android.view.View;
// Importación para el contenedor que agrupa y organiza otras vistas
import android.view.ViewGroup;
// Importación específica para gestionar las reglas de diseño en contenedores lineales
import android.widget.LinearLayout;
// Importación para el componente encargado de mostrar texto en pantalla
import android.widget.TextView;
// Importación para indicar que un parámetro o método no debe recibir valores nulos
import androidx.annotation.NonNull;
// Importación de la clase base para la gestión de listas optimizadas
import androidx.recyclerview.widget.RecyclerView;
// Importación de la interfaz para el manejo de colecciones de datos ordenadas
import java.util.List;
// Importación del acceso a los recursos generados del proyecto
import es.medac.skycollectorapp.R;
// Importación del modelo de datos que representa la estructura de un mensaje
import es.medac.skycollectorapp.models.Mensaje;

// Definición de la clase del adaptador que gestiona la visualización de la conversación
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    // Lista en memoria que almacena la colección de objetos de mensaje
    private List<Mensaje> listaMensajes;

    // Constructor de la clase que recibe e inicializa la lista de mensajes
    public ChatAdapter(List<Mensaje> listaMensajes) {
        // Asignación de la lista de mensajes externa a la variable local
        this.listaMensajes = listaMensajes;
    }

    // Método encargado de crear y configurar el contenedor visual para cada mensaje
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Generación del objeto de vista a partir del diseño XML específico del chat
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        // Retorno de una nueva instancia del contenedor de vistas del chat
        return new ChatViewHolder(view);
    }

    // Método que asocia los datos del mensaje con los componentes visuales en cada posición
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        // Recuperación del objeto mensaje correspondiente a la posición actual de la lista
        Mensaje msg = listaMensajes.get(position);
        // Establecimiento del texto del mensaje en el componente visual
        holder.txtMensaje.setText(msg.getContenido());

        // Obtención de los parámetros de diseño actuales del componente de texto
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.txtMensaje.getLayoutParams();

        // Estructura condicional para diferenciar los mensajes propios de los recibidos
        if (msg.isEsMio()) {
            // Alineación del mensaje a la derecha del contenedor para el usuario
            params.gravity = Gravity.END;
            // Aplicación de un color de fondo verde mediante código hexadecimal
            holder.txtMensaje.setBackgroundColor(Color.parseColor("#00C853"));
            // Establecimiento del color de fuente a blanco para mejorar el contraste
            holder.txtMensaje.setTextColor(Color.WHITE);
        } else {
            // Alineación del mensaje a la izquierda del contenedor para el bot
            params.gravity = Gravity.START;
            // Aplicación de un color de fondo gris oscuro para identificar al bot
            holder.txtMensaje.setBackgroundColor(Color.parseColor("#424242"));
            // Establecimiento del color de fuente a blanco
            holder.txtMensaje.setTextColor(Color.WHITE);
        }
        // Aplicación de los parámetros de diseño modificados al componente de texto
        holder.txtMensaje.setLayoutParams(params);
    }

    // Método que devuelve la cantidad total de mensajes registrados en la lista
    @Override
    public int getItemCount() { return listaMensajes.size(); }

    // Clase interna que define y vincula los componentes visuales de un mensaje individual
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        // Declaración del componente de texto para el cuerpo del mensaje
        TextView txtMensaje;
        // Constructor del contenedor que asocia el componente con su ID del XML
        public ChatViewHolder(@NonNull View itemView) {
            // Llamada al constructor de la clase base con la vista recibida
            super(itemView);
            // Vinculación de la variable de texto con el elemento del diseño XML
            txtMensaje = itemView.findViewById(R.id.txtMensaje);
        }
    }
}