# athento-nx-afm

## What is it?

This plugin is used to upload files to a CMIS folder from a filesystem folder.

## How to use it?

In configuration file the following properties must be created:

    ${ATHENTO_HOST}/bin/nuxeo.conf

Number of monitoring folders (N):

    AFM.NumberOfFolders=<N>

Each folder configuration into nuxeo.conf:

    AFM.PathDocuments.1=/home/example/example_folder_1/ 
    AFM.PathDocuments.2=/home/example/example_folder_2/
    ... 
    AFM.PathDocuments.N=/home/example/example_folder_N/ 

Each folder into FileSystem needs a workspace path into Athento ECM:

    AFM.PathWorkspace.1=/default-domain/workspaces/work_example_1
    AFM.PathWorkspace.2=/default-domain/workspaces/work_example_2
    ...
    AFM.PathWorkspace.N=/default-domain/workspaces/work_example_N

If you need you can set the folder document id:

    AFM.IdWorkspace.1=<docId>
    ...

You can define your document type to create from FileSystem:

    AFM.DocumentType=<Type>


### TODO

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

### Contact

 * You can contact anybody at http://www.athento.com/contact

 * You can also write us to soporte@athento.com
