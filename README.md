# athento-nx-afm

What is it?
-----------

This plugin is used to upload files to a CMIS folder from a filesystem folder.

How to use it?
--------------

In nuxeo.properties the following properties must be created:

# Cuantas carpetas del FS estan siendo monitorizadas (N)
AFM.NumberOfFolders=N

#Para cada carpeta, ruta en el FS
AFM.PathDocuments.1=/home/example/example_folder_1/ 
AFM.PathDocuments.2=/home/example/example_folder_2/
... 
AFM.PathDocuments.N=/home/example/example_folder_N/ 

#Para cada carpeta, ruta del workspace  (Sin el ultimo y colocando exactamente la misma ruta que se observa en la URL de Nuxeo dentro de ese WorkSpace)
AFM.PathWorkspace.1=/default-domain/workspaces/work_example_1
AFM.PathWorkspace.2=/default-domain/workspaces/work_example_2
...
AFM.PathWorkspace.N=/default-domain/workspaces/work_example_N

#Para cada carpeta es necesario colocar el ID del workspace
AFM.IdWorkspace.1=<docId>
...

# Si se define la propiedad, el tipo de documento indicado, será el que se cree, en caso contrario se creara un File.
AFM.DocumentType = Type


TODO
----

 * Refactor this component so that source and destiny folder are managed from data base.
 * Refactor component so that "AFM.NumberOfFolders" property is not needed.
 * Add a new configuration property to choose what order files should be read: alphabetical by name, creation date, size, extension, modification date
 * Add a new configuration property for the queuing policy: FIFO, LIFO, random, etc.
 * Add multithread support so that several files may be uploaded at once. Of course, keep control of conflicts, etc.
 * Add a new configuration property to choose if files uploaded will be: removed, moved to another folder or renamed.
 * Add checker to avoid blocking of this component (when there are files to upload, the component should upload them without any exception or blocking).
 * Number of threads per monitored folder (1 by default).
 * File extension (.* by default).
 * Blocking Check: Max idle time per file (if a file waits fo more than X minutes/seconds, an alert will raise).
 * Blocking Check Alert: WS URL or mail address to notify.


Latest version
--------------

Details of the latest version can be found at http://doc.athento.com 

Documentation
-------------

The documentation available can be found at http://doc.athento.com

Instalation
-----------

Please see the file called INSTALL or http://doc.athento.com

Licensing
---------

Please see the file called LICENSE

Contact
-------

 * Ask for help at http://answers.athento.com

 * You can contact anybody at http://www.athento.com/contact

 * You can also write us to support@athento.com
