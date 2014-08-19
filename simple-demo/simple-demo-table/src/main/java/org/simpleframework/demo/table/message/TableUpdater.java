package org.simpleframework.demo.table.message;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.simpleframework.demo.table.Query;
import org.simpleframework.demo.table.TableCursor;
import org.simpleframework.demo.table.TableModel;
import org.simpleframework.demo.table.TableSubscription;
import org.simpleframework.demo.table.extract.RowExtractor;
import org.simpleframework.demo.table.format.RowFormatter;
import org.simpleframework.demo.table.schema.TableSchema;
import org.simpleframework.http.socket.WebSocket;
import org.simpleframework.util.thread.Daemon;

public class TableUpdater extends Daemon {   

   private final Set<TableConnection> connections;   
   private final RowExtractor extractor;
   private final RowFormatter formatter;
   private final TableSchema schema;
   private final TableModel model;
   
   public TableUpdater(TableModel model, TableSchema schema, RowExtractor extractor, RowFormatter formatter) {
      this.connections = new CopyOnWriteArraySet<TableConnection>();
      this.formatter = formatter;
      this.extractor = extractor;      
      this.schema = schema;
      this.model = model;      
   }
   
   public void refresh() {
      for(TableConnection connection : connections) {
         try {
            connection.refresh();
         } catch(Exception e) {
            e.printStackTrace();
         }
      }
   }
  

   public void subscribe(WebSocket socket, Query client) {
      TableSubscription subscription = model.subscribe(client);
      
      if(subscription != null) {
         TableSession session = new TableSession(socket);
         TableCursor cursor = new TableCursor(subscription, schema, extractor, formatter);
         TableConnection connection = new TableConnection(cursor, session, schema);
         
         connections.add(connection);
      }
   }
   
   public void run() {
      while(true) {
         try {
            Thread.sleep(50);
         
            for(TableConnection connection : connections) {
               long time = System.currentTimeMillis();
               try {
                  connection.update();
               }catch(Exception e){
                  System.err.println("ERROR AFTER " +(System.currentTimeMillis() - time) + " " + e);
                  //e.printStackTrace();
                  connections.remove(connection);
               }
            }
         } catch(Exception e) {
            e.printStackTrace();
         }
      }
   }
      
      
   
}
