
#include <QtCore/QCoreApplication>
#include <iostream>
#include <stdlib.h>
#include <boost/filesystem.hpp>
#include "globals.h"
#include "globalclient.h"
#include "Reader.h"
#include "Server.h"

int main(int argc, char *argv[]){
    QCoreApplication a(argc, argv);    

    glb::mainThread = a.thread();
	glbClient::client->setHostAndPort(glb::host, glb::port);
	glbClient::client->init();

    std::cout << std::endl << "Oi, mate!";

    qRegisterMetaType<QAbstractSocket::SocketError>();
    qRegisterMetaType<QAbstractSocket::SocketState>();

    //set current working directory to executable path
    //needed if started from within IDE
    boost::filesystem::path curDir(QCoreApplication::applicationDirPath().toStdString());
    boost::filesystem::current_path(curDir);

    srand(time(NULL));

    int alg = 0;

    if (argc < 2){
        std::cout << std::endl << "No algorithm specified -> using Central Mutual Exclusion algorithm";
    }
    else{
        QString inputAlg = argv[1];

        if (inputAlg == "RA"){
            std::cout << std::endl << "Ricart Agrawala algorithm selected";
            alg = 1;
        }
        else if (inputAlg == "CME"){
            std::cout << std::endl << "Central Mutual Exclusion algorithm selected";
        }
        else{
            std::cout << std::endl << "unknown algorithm specified -> using Central Mutual Exclusion algorithm";
        }
    }

    if (alg == 1){

    }
    else{
        //init the Server
        Server* server = new Server();
        server->init();

        Reader* reader = new Reader();
        reader->start();
    }

    return a.exec();
}
