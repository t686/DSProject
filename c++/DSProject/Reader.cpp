#include "Reader.h"

Reader::Reader() : QThread(){

    connect(this,SIGNAL(join(QString)),glbClient::client,SLOT(join(QString)),Qt::UniqueConnection);
    connect(this,SIGNAL(signOff()),glbClient::client,SLOT(signOff()),Qt::UniqueConnection);
    connect(this,SIGNAL(startElection()),glbClient::client,SLOT(startElection()),Qt::UniqueConnection);
    connect(this,SIGNAL(startConcatProcess()),glbClient::client,SLOT(startConcatProcess()),Qt::UniqueConnection);
    connect(this,SIGNAL(stopOperations()),glbClient::client,SLOT(stopOperations()),Qt::UniqueConnection);
    connect(this,SIGNAL(runOverRpc(QString,QList<QVariant>)),glbClient::client,SLOT(runOverRpc(QString,QList<QVariant>)),Qt::UniqueConnection);
    connect(this,SIGNAL(showAllLists()),glbClient::client,SLOT(showAllLists()),Qt::UniqueConnection);
	connect(this, SIGNAL(echo(QString, QString)), glbClient::client, SLOT(echo(QString, QString)), Qt::UniqueConnection);
	connect(glbClient::client, SIGNAL(finishedTask()), this, SLOT(clientFinished()), Qt::UniqueConnection);

	busy = false;
}

void Reader::run(){
	//Infinite loop for listening to input characters
    std::cout << std::endl << "List of options: join, signoff, start, bully, host, list, echo, exit"<< std::endl;
	while (true){
		if (!busy){
			std::string str;
			std::getline(std::cin, str);
			QString inputText(str.c_str());
			std::cout << std::endl << "> " << inputText.toStdString();
			selectedOption(inputText);
		}
	}
}

void Reader::selectedOption(QString option){
	busy = true;
	if (option == "join"){
        std::cout << std::endl << "Enter IP:Port"<< std::endl;
		std::string str;
        std::getline(std::cin, str);
        QString newNodeIP(str.c_str());
        emit join(newNodeIP);
	}
	else if (option == "signoff"){
		//System.out.println("Operation \"Sign Off\" initiated.");
        emit signOff();
	}
	else if (option == "start"){
        std::cout << std::endl << "concatenation process initiated.";
        emit startConcatProcess();
	}
	else if (option == "bully"){
        std::cout << std::endl << "Host election initiated.";
        emit startElection();
	}
	else if (option == "host"){
		std::cout << std::endl << glb::host.toStdString() << " is the current host";
		busy = false;
	}
	else if (option == "list"){
        emit showAllLists();
	}
    else if (option == "echo"){
        std::cout << std::endl << "Enter IP:Port"<< std::endl;
        std::string str;
        std::getline(std::cin, str);
        QString newNodeIP(str.c_str());
        std::cout << std::endl << "Enter echo msg"<< std::endl;
        std::string str2;
        std::getline(std::cin, str2);
        QString echoStr(str2.c_str());
        emit echo(newNodeIP,echoStr);
    }
	else if (option == "exit"){
        std::cout << std::endl << "Quiting the program...";
        emit signOff();
		QCoreApplication::exit();
	}
	else{
		std::cout << std::endl << "Pardon, wrong input." << std::endl;
		busy = false;
    }
}

void Reader::clientFinished(){
	busy = false;
}