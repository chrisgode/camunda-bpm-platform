'use strict';

var usersPage = require('../pages/users');

describe('admin user -', function() {

  describe('start test', function() {

    it('should login', function() {

      usersPage.navigateToWebapp('Admin');

      usersPage.login('jonny1', 'jonny1');

    });

  });


  describe('remove current admin user rights', function() {

    it('should select user', function() {

      // when
      usersPage.selectUser(2);

      // then
      expect(usersPage.editUserGroups.pageHeader()).toBe('Jonny Prosciutto');

    });


    it('should remove admin group and log out', function() {

      // given
      usersPage.editUserGroups.selectUserNavbarItem('Groups');

      // when
      usersPage.editUserGroups.removeGroup(0);

      // then
      expect(usersPage.editUserGroups.groupList().count()).toEqual(0);

    });

  });


  describe('validate intial admin setup', function() {

    it('should validate Setup page', function() {

      // given
      usersPage.logoutWebapp();

      // when
      usersPage.navigateToWebapp('Admin');

      // then
      expect(usersPage.adminUserSetup.pageHeader()).toBe('Setup');
      expect(usersPage.adminUserSetup.createNewAdminButton().isEnabled()).toBe(false);
    });


    it('should enter new admin profile', function() {

      // when
      usersPage.adminUserSetup.userId().sendKeys('Admin');
      usersPage.adminUserSetup.password().sendKeys('admin123');
      usersPage.adminUserSetup.passwordRepeat().sendKeys('admin123');
      usersPage.adminUserSetup.userFirstName().sendKeys('Über');
      usersPage.adminUserSetup.userLastName().sendKeys('Admin');
      usersPage.adminUserSetup.userEmail().sendKeys('uea@camundo.org');

      usersPage.adminUserSetup.createNewAdminButton().click();

      // then
      expect(usersPage.adminUserSetup.Status.statusMessage()).toBe('User created You have created an initial user.');

    });


    it('should login page as Admin', function() {

      // when
      usersPage.navigateToWebapp('Admin');
      usersPage.login('Admin', 'admin123');

      // then
      expect(usersPage.userFirstNameAndLastName(0)).toBe('Über Admin');

    });

  });


  describe('reassign admin user rights', function() {

    it('should open group select page', function() {

      // given
      usersPage.selectUser(3);
      usersPage.editUserGroups.selectUserNavbarItem('Groups');

      // when
      usersPage.editUserGroups.addGroupButton().click();

      // then
      expect(usersPage.editUserGroups.selectGroup.pageHeader()).toBe('Select Groups');

    });


    it('should add camunda-admin group', function() {

      // when
      usersPage.editUserGroups.selectGroup.addGroup(1);

      // then
      expect(usersPage.editUserGroups.groupList().count()).toEqual(1);

    });

  });


  describe('remove interim admin', function() {

    it('should delete user account', function() {

      // given
      usersPage.navigateTo();
      usersPage.selectUser(0);
      usersPage.editUserAccount.selectUserNavbarItem('Account');

      // when
      usersPage.editUserAccount.deleteUserButton().click();
      usersPage.editUserAccount.deleteUserAlert().accept();

      // then
      expect(usersPage.userList().count()).toEqual(5);

     });

  });

  describe('end test', function() {

    it('should log out', function() {

      usersPage.logoutWebapp();

    })

  });

});