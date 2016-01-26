
#include <QCoreApplication>
#include <iostream>
#include <stdlib.h>
#include <boost/filesystem.hpp>
#include "globals.h"
#include "Client.h"
#include "Reader.h"
#include "Server.h"

void myMessageOutput(QtMsgType type, const QMessageLogContext &context, const QString &msg){
	QByteArray localMsg = msg.toLocal8Bit();
	switch (type) {
	case QtDebugMsg:
		fprintf(stderr, "\n***\nDebug: %s (%s:%u, %s)\n***\n", localMsg.constData(), context.file, context.line, context.function);
		break;
	case QtInfoMsg:
		fprintf(stderr, "\n***\nInfo: %s (%s:%u, %s)\n***\n", localMsg.constData(), context.file, context.line, context.function);
		break;
	case QtWarningMsg:
		fprintf(stderr, "\n***\nWarning: %s (%s:%u, %s)\n***\n", localMsg.constData(), context.file, context.line, context.function);
		break;
	case QtCriticalMsg:
		//Filter out multiple protocol start... its a library bug
		if (!QString::fromStdString(localMsg.toStdString()).contains("Multiple proto"))
			fprintf(stderr, "\n***\nCritical: %s (%s:%u, %s)\n***\n", localMsg.constData(), context.file, context.line, context.function);
		break;
	case QtFatalMsg:
		fprintf(stderr, "\n***\nFatal: %s (%s:%u, %s)\n***\n", localMsg.constData(), context.file, context.line, context.function);
		abort();
	}
}

int main(int argc, char *argv[]){
	qInstallMessageHandler(myMessageOutput);
    QCoreApplication a(argc, argv);    

    glb::mainThread = a.thread();	

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

		Reader* reader = new Reader(server->getClient());
		reader->start();
    }

    return a.exec();
}
